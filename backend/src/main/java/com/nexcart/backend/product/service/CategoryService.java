package com.nexcart.backend.product.service;

import com.nexcart.backend.product.domain.Category;
import com.nexcart.backend.product.dto.CategorySummary;
import java.util.List;
import java.util.UUID;

public interface CategoryService {

	List<CategorySummary> list();

	/**
	 * Resolves a category id to its entity, throwing NotFoundException.category if it doesn't exist.
	 * Used internally by ProductService when creating/updating a product.
	 */
	Category getEntityById(UUID id);
}
