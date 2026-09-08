package com.nexcart.backend.inventory.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record InventoryAdjustRequest(
	@NotNull(message = "Available quantity is required")
	@PositiveOrZero(message = "Available quantity cannot be negative")
	Integer availableQuantity
) {
}
