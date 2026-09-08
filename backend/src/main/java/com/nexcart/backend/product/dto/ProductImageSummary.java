package com.nexcart.backend.product.dto;

import com.nexcart.backend.product.domain.ProductImage;
import java.util.UUID;

public record ProductImageSummary(
	UUID id,
	String url,
	int displayOrder,
	boolean primary
) {

	public static ProductImageSummary from(ProductImage image) {
		return new ProductImageSummary(image.getId(), image.getUrl(), image.getDisplayOrder(), image.isPrimary());
	}
}
