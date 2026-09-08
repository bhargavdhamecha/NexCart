package com.nexcart.backend.product.service;

import com.nexcart.backend.common.dto.PageResponse;
import com.nexcart.backend.common.exception.NotFoundException;
import com.nexcart.backend.product.domain.Product;
import com.nexcart.backend.product.domain.ProductStatus;
import com.nexcart.backend.product.dto.ProductCatalogEntry;
import com.nexcart.backend.product.dto.ProductImageSummary;
import com.nexcart.backend.product.repository.ProductRepository;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProductCatalogCacheImpl implements ProductCatalogCache {

	private final ProductRepository productRepository;
	private final ProductImageService productImageService;

	// Explicit String keys throughout, deliberately not relying on Spring's default key
	// generator (a SimpleKey built from the raw parameters) — RedisCacheConfiguration is set up
	// to serialize cache keys with StringRedisSerializer, so the key value produced here must
	// always actually be a String, not an object we're hoping gets converted correctly.
	//
	// Null-safe via ternaries, NOT T(String).valueOf(...) — SpEL's reflective overload
	// resolution for a null argument to an overloaded static method like String.valueOf can
	// pick the wrong overload (valueOf(char[]) instead of valueOf(Object)), which then NPEs
	// trying to read the "array"'s length. Confirmed by hitting exactly that NPE here.
	@Override
	@Transactional(readOnly = true)
	@Cacheable(cacheNames = "productList",
		key = "(#categoryId == null ? 'null' : #categoryId.toString()) + '|' + "
			+ "(#search == null ? 'null' : #search) + '|' + "
			+ "#pageable.pageNumber + '|' + #pageable.pageSize + '|' + #pageable.sort.toString()")
	public PageResponse<ProductCatalogEntry> getPage(UUID categoryId, String search, Pageable pageable) {
		Page<Product> page = productRepository.search(ProductStatus.ACTIVE, categoryId, toSearchPattern(search),
			pageable);

		List<UUID> productIds = page.getContent().stream().map(Product::getId).toList();
		Map<UUID, List<ProductImageSummary>> imagesByProductId = productImageService.listByProducts(productIds);

		return PageResponse.from(page.map(product ->
			ProductCatalogEntry.from(product, imagesByProductId.getOrDefault(product.getId(), List.of()))));
	}

	@Override
	@Transactional(readOnly = true)
	@Cacheable(cacheNames = "productDetail", key = "#productId.toString()")
	public ProductCatalogEntry getEntry(UUID productId) {
		Product product = productRepository.findById(productId)
			.orElseThrow(() -> NotFoundException.product(productId));
		return ProductCatalogEntry.from(product, productImageService.listByProduct(productId));
	}

	private String toSearchPattern(String search) {
		return (search == null || search.isBlank()) ? null : "%" + search.trim().toLowerCase() + "%";
	}
}
