package com.nexcart.backend.inventory.dto;

import com.nexcart.backend.inventory.domain.Inventory;
import java.time.Instant;
import java.util.UUID;

public record InventorySummary(
	UUID productId,
	Integer availableQuantity,
	Integer reservedQuantity,
	Instant updatedAt
) {

	public static InventorySummary from(Inventory inventory) {
		return new InventorySummary(inventory.getProductId(), inventory.getAvailableQuantity(),
			inventory.getReservedQuantity(), inventory.getUpdatedAt());
	}
}
