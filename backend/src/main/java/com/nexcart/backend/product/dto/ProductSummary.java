package com.nexcart.backend.product.dto;

import com.nexcart.backend.product.domain.Product;
import com.nexcart.backend.product.domain.ProductStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ProductSummary(
	UUID productId,
	String title,
	String description,
	BigDecimal price,
	ProductStatus status,
	CategorySummary category,
	Integer availableQuantity,
	List<ProductImageSummary> images,
	Instant createdAt,
	Instant updatedAt
) {

	public static ProductSummary from(Product product, Integer availableQuantity, List<ProductImageSummary> images) {
		return new ProductSummary(
			product.getId(),
			product.getTitle(),
			product.getDescription(),
			product.getPrice(),
			product.getStatus(),
			CategorySummary.from(product.getCategory()),
			availableQuantity,
			images,
			product.getCreatedAt(),
			product.getUpdatedAt()
		);
	}

	/** The primary image's URL, falling back to the first uploaded image, or null if none exist. */
	public String primaryImageUrl() {
		return images.stream()
			.filter(ProductImageSummary::primary)
			.findFirst()
			.or(() -> images.stream().findFirst())
			.map(ProductImageSummary::url)
			.orElse(null);
	}
}
