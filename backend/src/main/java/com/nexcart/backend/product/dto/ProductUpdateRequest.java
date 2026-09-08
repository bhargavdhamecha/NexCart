package com.nexcart.backend.product.dto;

import com.nexcart.backend.product.domain.ProductStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.UUID;

public record ProductUpdateRequest(
	@NotBlank(message = "Title is required")
	@Size(max = 200, message = "Title must be at most 200 characters")
	String title,

	@NotBlank(message = "Description is required")
	@Size(max = 2000, message = "Description must be at most 2000 characters")
	String description,

	@NotNull(message = "Price is required")
	@DecimalMin(value = "0.0", inclusive = false, message = "Price must be greater than 0")
	BigDecimal price,

	@NotNull(message = "Category is required")
	UUID categoryId,

	@NotNull(message = "Status is required")
	ProductStatus status
) {
}
