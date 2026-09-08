package com.nexcart.backend.cart.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.UUID;

public record AddCartItemRequest(
	@NotNull(message = "Product is required")
	UUID productId,

	@NotNull(message = "Quantity is required")
	@Positive(message = "Quantity must be at least 1")
	Integer quantity
) {
}
