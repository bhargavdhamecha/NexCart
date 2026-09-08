package com.nexcart.backend.order.service;

import com.nexcart.backend.order.dto.OrderSummary;
import java.util.List;
import java.util.UUID;

public interface OrderService {

	/**
	 * Converts the authenticated user's server-side cart into an Order: revalidates each
	 * product's current price/ACTIVE status, decrements inventory (simple, transaction-scoped —
	 * not concurrency-hardened, see InventoryService.decrease), and clears the cart. Throws
	 * BadRequestException.emptyCart if the cart has no items.
	 */
	OrderSummary checkout(UUID userId);

	List<OrderSummary> getOrders(UUID userId);

	/** Ownership-scoped — throws NotFoundException.order if the order isn't the caller's. */
	OrderSummary getOrder(UUID userId, UUID orderId);

	/**
	 * Locked variant of getOrder() — used only by PaymentServiceImpl.initiate(), so a concurrent
	 * initiate() call for the same order (e.g. two browser tabs) serializes instead of racing to
	 * both create a Payment. Must be the FIRST read of this Order within its transaction —
	 * locking after an earlier unlocked read of the same entity can return a stale
	 * first-level-cache object instead of a freshly-locked one.
	 */
	OrderSummary getOrderForUpdate(UUID userId, UUID orderId);

	// Exposed specifically for PaymentServiceImpl, so it never reaches into OrderRepository
	// directly (same cross-module discipline as everywhere else in this codebase).
	void markPendingPayment(UUID orderId, UUID paymentId);

	void markConfirmed(UUID orderId);

	void markPaymentFailed(UUID orderId);
}
