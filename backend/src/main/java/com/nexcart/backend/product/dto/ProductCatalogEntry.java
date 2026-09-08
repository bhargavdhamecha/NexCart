package com.nexcart.backend.product.dto;

import com.nexcart.backend.product.domain.Product;
import com.nexcart.backend.product.domain.ProductStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * The slow-changing "catalog" half of a product — everything ProductSummary has except live
 * availableQuantity. This is deliberately what ProductCatalogCache caches: stock changes on
 * every checkout and must never be stale, so it's fetched fresh and merged in separately (see
 * ProductServiceImpl) rather than being part of the cached shape — that keeps the cache from
 * needing to be invalidated on every order.
 */
public record ProductCatalogEntry(
	UUID productId,
	String title,
	String description,
	BigDecimal price,
	ProductStatus status,
	CategorySummary category,
	List<ProductImageSummary> images,
	Instant createdAt,
	Instant updatedAt
) {

	public static ProductCatalogEntry from(Product product, List<ProductImageSummary> images) {
		return new ProductCatalogEntry(
			product.getId(),
			product.getTitle(),
			product.getDescription(),
			product.getPrice(),
			product.getStatus(),
			CategorySummary.from(product.getCategory()),
			images,
			product.getCreatedAt(),
			product.getUpdatedAt()
		);
	}

	public ProductSummary withAvailableQuantity(Integer availableQuantity) {
		return new ProductSummary(productId, title, description, price, status, category, availableQuantity, images,
			createdAt, updatedAt);
	}
}
