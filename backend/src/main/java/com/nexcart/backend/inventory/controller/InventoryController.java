package com.nexcart.backend.inventory.controller;

import com.nexcart.backend.inventory.dto.InventoryAdjustRequest;
import com.nexcart.backend.inventory.dto.InventorySummary;
import com.nexcart.backend.inventory.service.InventoryService;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/products/{productId}/inventory")
@RequiredArgsConstructor
public class InventoryController {

	private final InventoryService inventoryService;

	@PatchMapping
	public InventorySummary adjust(@PathVariable UUID productId, @Valid @RequestBody InventoryAdjustRequest request) {
		return inventoryService.adjustStock(productId, request.availableQuantity());
	}
}
