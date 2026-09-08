package com.nexcart.backend.product.dto;

import com.nexcart.backend.product.domain.Category;
import java.util.UUID;

public record CategorySummary(
	UUID id,
	String name
) {

	public static CategorySummary from(Category category) {
		return new CategorySummary(category.getId(), category.getName());
	}
}
