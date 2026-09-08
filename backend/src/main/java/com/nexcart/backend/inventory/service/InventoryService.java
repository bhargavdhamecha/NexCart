package com.nexcart.backend.inventory.service;

import com.nexcart.backend.inventory.dto.InventorySummary;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;

public interface InventoryService {

	/** Creates the 1:1 Inventory row for a newly created product. */
	void initialize(UUID productId, int availableQuantity);

	/** Returns just the available quantity, throwing NotFoundException.inventory if missing. */
	Integer getAvailableQuantity(UUID productId);

	/** Batched variant of getAvailableQuantity — one query for a whole page of products, used by
	 *  ProductServiceImpl so stock can be merged in fresh without an N+1, and without ever
	 *  needing the product catalog cache to be invalidated on a stock change. A product with no
	 *  Inventory row is simply absent from the returned map (unlike the single-id variant, this
	 *  doesn't throw — callers default missing entries as they see fit). */
	Map<UUID, Integer> getAvailableQuantities(Collection<UUID> productIds);

	InventorySummary adjustStock(UUID productId, int availableQuantity);

	/**
	 * Relative decrement used at checkout — throws ConflictException.insufficientStock if there
	 * isn't enough. This is a plain transaction-scoped read-modify-write, not a concurrency-safe
	 * reservation: two simultaneous checkouts for the last unit can still both succeed under
	 * READ_COMMITTED. True oversell-prevention is out of scope for this pass (see context.md's
	 * own separate "Concurrency handling" step).
	 */
	void decrease(UUID productId, int quantity);

	/** Relative restore — used to undo a checkout's decrease() when a payment fails. */
	void increase(UUID productId, int quantity);
}
