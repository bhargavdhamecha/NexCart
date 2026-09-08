package com.nexcart.backend.product.service;

import com.nexcart.backend.common.dto.PageResponse;
import com.nexcart.backend.common.exception.NotFoundException;
import com.nexcart.backend.inventory.service.InventoryService;
import com.nexcart.backend.product.domain.Category;
import com.nexcart.backend.product.domain.Product;
import com.nexcart.backend.product.domain.ProductStatus;
import com.nexcart.backend.product.dto.ProductCatalogEntry;
import com.nexcart.backend.product.dto.ProductCreateRequest;
import com.nexcart.backend.product.dto.ProductSummary;
import com.nexcart.backend.product.dto.ProductUpdateRequest;
import com.nexcart.backend.product.repository.ProductRepository;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

	private final ProductRepository productRepository;
	private final CategoryService categoryService;
	private final InventoryService inventoryService;
	private final ProductImageService productImageService;
	private final ProductCatalogCache productCatalogCache;

	// Catalog data (title/description/price/category/images/status) comes from the cache; live
	// stock never does — it's fetched fresh in one batched call and merged in here, so a
	// checkout anywhere never needs to invalidate this cache (see ProductCatalogEntry's javadoc).
	@Override
	@Transactional(readOnly = true)
	public PageResponse<ProductSummary> list(UUID categoryId, String search, Pageable pageable) {
		PageResponse<ProductCatalogEntry> page = productCatalogCache.getPage(categoryId, search, pageable);
		Map<UUID, Integer> stockByProductId = inventoryService.getAvailableQuantities(
			page.content().stream().map(ProductCatalogEntry::productId).toList());

		List<ProductSummary> content = page.content().stream()
			.map(entry -> entry.withAvailableQuantity(stockByProductId.get(entry.productId())))
			.toList();
		return new PageResponse<>(content, page.totalElements(), page.totalPages(), page.number(), page.size());
	}

	@Override
	@Transactional(readOnly = true)
	public ProductSummary getById(UUID id) {
		ProductCatalogEntry entry = productCatalogCache.getEntry(id);
		return entry.withAvailableQuantity(inventoryService.getAvailableQuantity(id));
	}

	@Override
	@Transactional
	@CacheEvict(cacheNames = "productList", allEntries = true)
	public ProductSummary create(ProductCreateRequest request) {
		Category category = categoryService.getEntityById(request.categoryId());
		Product product = Product.create(request.title().trim(), request.description().trim(), request.price(),
			category);
		product = productRepository.save(product);
		inventoryService.initialize(product.getId(), request.initialQuantity());
		return toSummary(product);
	}

	@Override
	@Transactional
	@Caching(evict = {
		@CacheEvict(cacheNames = "productDetail", key = "#id.toString()"),
		@CacheEvict(cacheNames = "productList", allEntries = true)
	})
	public ProductSummary update(UUID id, ProductUpdateRequest request) {
		Product product = findById(id);
		Category category = categoryService.getEntityById(request.categoryId());
		product.setTitle(request.title().trim());
		product.setDescription(request.description().trim());
		product.setPrice(request.price());
		product.setCategory(category);
		product.setStatus(request.status());
		return toSummary(product);
	}

	@Override
	@Transactional
	@Caching(evict = {
		@CacheEvict(cacheNames = "productDetail", key = "#id.toString()"),
		@CacheEvict(cacheNames = "productList", allEntries = true)
	})
	public void delete(UUID id) {
		Product product = findById(id);
		product.setStatus(ProductStatus.INACTIVE);
	}

	private Product findById(UUID id) {
		return productRepository.findById(id).orElseThrow(() -> NotFoundException.product(id));
	}

	// Used only by create()/update() for their own return value — a freshly-mutated entity, not
	// something that should ever go through (or be confused with) the catalog cache. Must run
	// inside this method's own @Transactional boundary — Product.category is a LAZY association
	// and open-in-view is disabled, so it can't be touched later (e.g. in the controller)
	// without a LazyInitializationException.
	private ProductSummary toSummary(Product product) {
		Integer availableQuantity = inventoryService.getAvailableQuantity(product.getId());
		return ProductSummary.from(product, availableQuantity, productImageService.listByProduct(product.getId()));
	}
}
