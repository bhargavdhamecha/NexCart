package com.nexcart.backend.product.service;

import com.nexcart.backend.common.exception.BadRequestException;
import com.nexcart.backend.common.exception.NotFoundException;
import com.nexcart.backend.common.image.ImageCompressor;
import com.nexcart.backend.common.storage.StorageService;
import com.nexcart.backend.product.domain.Product;
import com.nexcart.backend.product.domain.ProductImage;
import com.nexcart.backend.product.dto.ProductImageSummary;
import com.nexcart.backend.product.repository.ProductImageRepository;
import com.nexcart.backend.product.repository.ProductRepository;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class ProductImageServiceImpl implements ProductImageService {

	// A deliberate small business rule given B2's free-tier storage is finite.
	private static final int MAX_IMAGES_PER_PRODUCT = 6;
	private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of("image/jpeg", "image/png", "image/webp");

	private final ProductImageRepository productImageRepository;
	private final ProductRepository productRepository;
	private final StorageService storageService;
	private final ImageCompressor imageCompressor;

	// An image change affects both this product's detail view and any listing showing its
	// thumbnail — evict both cache regions, matching ProductServiceImpl's update()/delete().
	@Override
	@Transactional
	@Caching(evict = {
		@CacheEvict(cacheNames = "productDetail", key = "#productId"),
		@CacheEvict(cacheNames = "productList", allEntries = true)
	})
	public ProductImageSummary upload(UUID productId, MultipartFile file) {
		Product product = productRepository.findById(productId)
			.orElseThrow(() -> NotFoundException.product(productId));

		String contentType = file.getContentType();
		if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType)) {
			throw BadRequestException.invalidImageFile("Only JPEG, PNG, or WEBP images are allowed");
		}

		long existingCount = productImageRepository.countByProductId(productId);
		if (existingCount >= MAX_IMAGES_PER_PRODUCT) {
			throw BadRequestException.tooManyImages(MAX_IMAGES_PER_PRODUCT);
		}

		ImageCompressor.CompressedImage compressed = imageCompressor.compress(file);
		String filename = baseFilename(file.getOriginalFilename()) + "." + compressed.extension();
		StorageService.UploadedFile uploaded = storageService.upload(compressed.bytes(), compressed.contentType(),
			filename, "products/" + productId);
		ProductImage image = ProductImage.create(product, uploaded.url(), uploaded.storageKey(),
			(int) existingCount, existingCount == 0);
		return ProductImageSummary.from(productImageRepository.save(image));
	}

	@Override
	@Transactional
	@Caching(evict = {
		@CacheEvict(cacheNames = "productDetail", key = "#productId"),
		@CacheEvict(cacheNames = "productList", allEntries = true)
	})
	public void delete(UUID productId, UUID imageId) {
		ProductImage image = productImageRepository.findByIdAndProductId(imageId, productId)
			.orElseThrow(() -> NotFoundException.productImage(imageId));
		storageService.delete(image.getStorageKey());
		productImageRepository.delete(image);
	}

	@Override
	@Transactional(readOnly = true)
	public List<ProductImageSummary> listByProduct(UUID productId) {
		return productImageRepository.findByProductIdOrderByDisplayOrderAsc(productId).stream()
			.map(ProductImageSummary::from)
			.toList();
	}

	@Override
	@Transactional(readOnly = true)
	public Map<UUID, List<ProductImageSummary>> listByProducts(Collection<UUID> productIds) {
		if (productIds.isEmpty()) {
			return Map.of();
		}
		// Group by product id BEFORE mapping to ProductImageSummary — that DTO doesn't carry a
		// product id (it's always scoped to one product by its caller), so grouping has to
		// happen on the raw entities. ProductImage.getProduct().getId() reads the id straight
		// off the (possibly still-lazy) proxy without triggering a fetch — Hibernate proxies
		// always know their own identifier.
		return productImageRepository.findByProductIdInOrderByDisplayOrderAsc(productIds).stream()
			.collect(Collectors.groupingBy(
				image -> image.getProduct().getId(),
				Collectors.mapping(ProductImageSummary::from, Collectors.toList())));
	}

	// Strips the original extension since ImageCompressor may re-encode the image (e.g. a
	// .png upload comes back as .jpg) — the stored filename should reflect the actual bytes.
	private String baseFilename(String originalFilename) {
		if (originalFilename == null || originalFilename.isBlank()) {
			return "image";
		}
		int dotIndex = originalFilename.lastIndexOf('.');
		return dotIndex > 0 ? originalFilename.substring(0, dotIndex) : originalFilename;
	}
}
