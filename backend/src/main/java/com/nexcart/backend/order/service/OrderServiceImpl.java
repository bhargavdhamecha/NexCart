package com.nexcart.backend.order.service;

import com.nexcart.backend.cart.dto.CartItemSummary;
import com.nexcart.backend.cart.dto.CartSummary;
import com.nexcart.backend.cart.service.CartService;
import com.nexcart.backend.common.event.EventPublisher;
import com.nexcart.backend.common.event.KafkaTopics;
import com.nexcart.backend.common.event.OrderConfirmedEvent;
import com.nexcart.backend.common.event.PaymentRequestedEvent;
import com.nexcart.backend.common.exception.BadRequestException;
import com.nexcart.backend.common.exception.ConflictException;
import com.nexcart.backend.common.exception.NotFoundException;
import com.nexcart.backend.inventory.service.InventoryService;
import com.nexcart.backend.order.domain.Order;
import com.nexcart.backend.order.domain.OrderItem;
import com.nexcart.backend.order.domain.OrderStatus;
import com.nexcart.backend.order.dto.OrderItemSummary;
import com.nexcart.backend.order.dto.OrderSummary;
import com.nexcart.backend.order.repository.OrderItemRepository;
import com.nexcart.backend.order.repository.OrderRepository;
import com.nexcart.backend.product.domain.ProductStatus;
import com.nexcart.backend.product.dto.ProductSummary;
import com.nexcart.backend.product.service.ProductService;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

	private final OrderRepository orderRepository;
	private final OrderItemRepository orderItemRepository;
	private final CartService cartService;
	private final ProductService productService;
	private final InventoryService inventoryService;
	private final EventPublisher eventPublisher;

	@Override
	@Transactional
	public OrderSummary checkout(UUID userId) {
		CartSummary cart = cartService.getCart(userId);
		if (cart.items().isEmpty()) {
			throw BadRequestException.emptyCart();
		}

		List<CheckoutLine> lines = new ArrayList<>();
		BigDecimal totalAmount = BigDecimal.ZERO;

		// Revalidate every line against current product data first (never trust anything
		// cached client-side) before touching inventory at all.
		for (CartItemSummary cartItem : cart.items()) {
			ProductSummary product = productService.getById(cartItem.productId());
			if (product.status() != ProductStatus.ACTIVE) {
				throw ConflictException.productUnavailable(cartItem.productId());
			}
			BigDecimal unitPrice = product.price();
			lines.add(new CheckoutLine(cartItem.productId(), product.title(), product.primaryImageUrl(),
				cartItem.quantity(), unitPrice));
			totalAmount = totalAmount.add(unitPrice.multiply(BigDecimal.valueOf(cartItem.quantity())));
		}

		// Simple, transaction-scoped decrement — not a concurrency-safe reservation (see
		// InventoryService.decrease). If any line is short, the whole @Transactional method
		// rolls back, so no partial decrements from this checkout persist.
		for (CheckoutLine line : lines) {
			inventoryService.decrease(line.productId(), line.quantity());
		}

		Order order = orderRepository.save(Order.create(userId, totalAmount));
		List<OrderItemSummary> itemSummaries = new ArrayList<>();
		for (CheckoutLine line : lines) {
			OrderItem item = orderItemRepository.save(OrderItem.create(order, line.productId(), line.title(),
				line.imageUrl(), line.quantity(), line.unitPrice()));
			itemSummaries.add(OrderItemSummary.from(item));
		}

		// Cart is intentionally left as-is here — it's only cleared once payment actually
		// succeeds (see PaymentServiceImpl.complete()), so a declined/abandoned payment doesn't
		// leave the user with an empty cart for items they never actually bought.

		eventPublisher.publish(KafkaTopics.PAYMENT_REQUESTED, order.getId().toString(),
			new PaymentRequestedEvent(order.getId(), userId, totalAmount));

		return OrderSummary.from(order, itemSummaries);
	}

	@Override
	@Transactional(readOnly = true)
	public List<OrderSummary> getOrders(UUID userId) {
		return orderRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
			.map(order -> OrderSummary.from(order, itemSummaries(order.getId())))
			.toList();
	}

	@Override
	@Transactional(readOnly = true)
	public OrderSummary getOrder(UUID userId, UUID orderId) {
		Order order = findOwned(userId, orderId);
		return OrderSummary.from(order, itemSummaries(order.getId()));
	}

	@Override
	@Transactional
	public OrderSummary getOrderForUpdate(UUID userId, UUID orderId) {
		Order order = orderRepository.findByIdAndUserIdForUpdate(orderId, userId)
			.orElseThrow(() -> NotFoundException.order(orderId));
		return OrderSummary.from(order, itemSummaries(order.getId()));
	}

	@Override
	@Transactional
	public void markPendingPayment(UUID orderId, UUID paymentId) {
		Order order = findById(orderId);
		// Idempotent: PaymentServiceImpl.initiate() calling this twice for the same order (its
		// own idempotency-key check already prevents that in practice, but defense in depth)
		// shouldn't be a hard error. PAYMENT_FAILED is also accepted here — a retried payment
		// (new idempotency key, after PaymentServiceImpl.initiate() re-decremented stock) moves
		// the order back into the payable flow the same way a fresh checkout does.
		if (order.getStatus() != OrderStatus.PENDING_PAYMENT) {
			if (order.getStatus() != OrderStatus.CREATED && order.getStatus() != OrderStatus.PAYMENT_FAILED) {
				throw ConflictException.invalidOrderState(
					"Cannot move order " + order.getId() + " to PENDING_PAYMENT from " + order.getStatus());
			}
			order.setStatus(OrderStatus.PENDING_PAYMENT);
		}
		order.setPaymentId(paymentId);
	}

	@Override
	@Transactional
	public void markConfirmed(UUID orderId) {
		Order order = findById(orderId);
		assertTransition(order, OrderStatus.PENDING_PAYMENT, OrderStatus.CONFIRMED);
		order.setStatus(OrderStatus.CONFIRMED);
		eventPublisher.publish(KafkaTopics.ORDER_CONFIRMED, orderId.toString(),
			new OrderConfirmedEvent(orderId, order.getUserId(), order.getTotalAmount()));
	}

	@Override
	@Transactional
	public void markPaymentFailed(UUID orderId) {
		Order order = findById(orderId);
		assertTransition(order, OrderStatus.PENDING_PAYMENT, OrderStatus.PAYMENT_FAILED);
		order.setStatus(OrderStatus.PAYMENT_FAILED);
	}

	// Explicit state-machine guard — combined with @Version optimistic locking on Order, this
	// makes a concurrent double-transition (e.g. two racing "complete payment" calls) fail
	// cleanly (409, see GlobalExceptionHandler) rather than one silently clobbering the other.
	private void assertTransition(Order order, OrderStatus requiredFrom, OrderStatus to) {
		if (order.getStatus() != requiredFrom) {
			throw ConflictException.invalidOrderState(
				"Cannot move order " + order.getId() + " to " + to + " from " + order.getStatus());
		}
	}

	private Order findById(UUID orderId) {
		return orderRepository.findById(orderId).orElseThrow(() -> NotFoundException.order(orderId));
	}

	private Order findOwned(UUID userId, UUID orderId) {
		return orderRepository.findByIdAndUserId(orderId, userId)
			.orElseThrow(() -> NotFoundException.order(orderId));
	}

	private List<OrderItemSummary> itemSummaries(UUID orderId) {
		return orderItemRepository.findByOrderIdOrderByCreatedAtAsc(orderId).stream()
			.map(OrderItemSummary::from)
			.toList();
	}

	private record CheckoutLine(UUID productId, String title, String imageUrl, int quantity, BigDecimal unitPrice) {
	}
}
