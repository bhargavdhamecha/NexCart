package com.nexcart.backend.payment.service;

import com.nexcart.backend.cart.service.CartService;
import com.nexcart.backend.common.event.EventPublisher;
import com.nexcart.backend.common.event.KafkaTopics;
import com.nexcart.backend.common.event.PaymentCompletedEvent;
import com.nexcart.backend.common.exception.ApiException;
import com.nexcart.backend.common.exception.ConflictException;
import com.nexcart.backend.common.exception.ErrorCode;
import com.nexcart.backend.common.exception.NotFoundException;
import com.nexcart.backend.inventory.service.InventoryService;
import com.nexcart.backend.order.domain.OrderItem;
import com.nexcart.backend.order.dto.OrderSummary;
import com.nexcart.backend.order.repository.OrderItemRepository;
import com.nexcart.backend.order.service.OrderService;
import com.nexcart.backend.payment.config.RazorpayProperties;
import com.nexcart.backend.payment.domain.Payment;
import com.nexcart.backend.payment.domain.PaymentStatus;
import com.nexcart.backend.payment.dto.PaymentInitiateRequest;
import com.nexcart.backend.payment.dto.PaymentSummary;
import com.nexcart.backend.payment.dto.PaymentVerifyRequest;
import com.nexcart.backend.payment.gateway.RazorpayGatewayClient;
import com.nexcart.backend.payment.repository.PaymentRepository;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

	private static final Set<String> PAYABLE_ORDER_STATUSES = Set.of("CREATED", "PENDING_PAYMENT", "PAYMENT_FAILED");

	private final PaymentRepository paymentRepository;
	private final OrderService orderService;
	private final OrderItemRepository orderItemRepository;
	private final InventoryService inventoryService;
	private final EventPublisher eventPublisher;
	private final CartService cartService;
	private final RazorpayGatewayClient razorpayGatewayClient;
	private final RazorpayProperties razorpayProperties;

	@Override
	@Transactional
	public PaymentSummary initiate(UUID userId, PaymentInitiateRequest request) {
		Payment existing = paymentRepository.findByIdempotencyKey(request.idempotencyKey()).orElse(null);
		if (existing != null) {
			return PaymentSummary.from(existing, razorpayProperties.getKeyId());
		}

		// Locked read: the first (and only) touch of this Order in this transaction, so a second
		// initiate() call for the same order — e.g. a second browser tab — blocks here rather
		// than racing this one to also create a Payment. Must stay the ONLY read: locking after
		// an earlier unlocked read of the same entity can return a stale first-level-cache
		// object instead of a freshly-locked one.
		OrderSummary order = orderService.getOrderForUpdate(userId, request.orderId());
		if (!PAYABLE_ORDER_STATUSES.contains(order.status())) {
			throw ConflictException.invalidOrderState(
				"Order " + order.orderId() + " is not payable in its current state: " + order.status());
		}

		// Another attempt (e.g. a different tab) already has a live payment going for this
		// order — reuse it rather than creating a competing Payment/Razorpay order. Race-free
		// because we're holding the Order row lock acquired above.
		Optional<Payment> pending = paymentRepository.findByOrderIdAndStatus(order.orderId(), PaymentStatus.INITIATED);
		if (pending.isPresent()) {
			return PaymentSummary.from(pending.get(), razorpayProperties.getKeyId());
		}

		// Retrying a previously-declined order: the earlier FAILED completion already restored
		// stock for these lines (see OrderServiceImpl.markPaymentFailed()'s callers), so
		// re-decrement it now before the retry is allowed to proceed — same insufficient-stock
		// guard as the original checkout applies.
		if ("PAYMENT_FAILED".equals(order.status())) {
			for (OrderItem item : orderItemRepository.findByOrderIdOrderByCreatedAtAsc(order.orderId())) {
				inventoryService.decrease(item.getProductId(), item.getQuantity());
			}
		}

		// The app-level check above is an optimization, not the actual safety net — under a
		// genuine race, two requests can both pass it before either commits. The DB's unique
		// constraint on idempotency_key is what's actually atomic: saveAndFlush forces the
		// constraint to be checked immediately (not deferred to end-of-transaction), so the
		// loser can catch the violation here and fall back to returning the winner's row,
		// honoring the idempotent contract even under real concurrency.
		Payment payment;
		try {
			payment = paymentRepository.saveAndFlush(Payment.create(order.orderId(), userId, order.totalAmount(),
				request.idempotencyKey(), request.paymentMethod()));
		}
		catch (DataIntegrityViolationException exception) {
			return paymentRepository.findByIdempotencyKey(request.idempotencyKey())
				.map(found -> PaymentSummary.from(found, razorpayProperties.getKeyId()))
				.orElseThrow(() -> exception);
		}

		// A gateway failure here rolls back the whole transaction, including the row just
		// flushed above — no orphan Payment left behind without a Razorpay order backing it.
		RazorpayGatewayClient.GatewayOrder gatewayOrder = razorpayGatewayClient.createOrder(
			payment.getAmount(), "INR", payment.getId().toString());
		payment.setRazorpayOrderId(gatewayOrder.orderId());

		orderService.markPendingPayment(order.orderId(), payment.getId());

		return PaymentSummary.from(payment, razorpayProperties.getKeyId());
	}

	@Override
	@Transactional
	public PaymentSummary verify(UUID userId, UUID paymentId, PaymentVerifyRequest request) {
		// Locked read: makes the terminal-state check below and the mutation that follows
		// atomic against a concurrent duplicate call for this same payment (see
		// PaymentRepository.findByIdAndUserIdForUpdate).
		Payment payment = paymentRepository.findByIdAndUserIdForUpdate(paymentId, userId)
			.orElseThrow(() -> NotFoundException.payment(paymentId));

		// Idempotent: a duplicate call against an already-terminal payment is a no-op, not an
		// error and not a re-application of side effects.
		if (payment.getStatus() == PaymentStatus.SUCCESS || payment.getStatus() == PaymentStatus.FAILED) {
			return PaymentSummary.from(payment, razorpayProperties.getKeyId());
		}

		// Verified against OUR stored razorpayOrderId — never a client-submitted one. See
		// RazorpayGatewayClient.verifySignature()'s javadoc for why that distinction matters.
		boolean verified = razorpayGatewayClient.verifySignature(
			payment.getRazorpayOrderId(), request.razorpayPaymentId(), request.razorpaySignature());

		if (!verified) {
			// Deliberately does not touch payment/order state — an invalid signature might be a
			// spoof attempt, not a genuine gateway failure. The user can just reopen Checkout and
			// legitimately retry against the same Razorpay order.
			throw new ApiException(HttpStatus.BAD_REQUEST, ErrorCode.PAYMENT_SIGNATURE_INVALID,
				"Payment verification failed");
		}

		payment.setStatus(PaymentStatus.SUCCESS);
		payment.setTransactionId(request.razorpayPaymentId());
		orderService.markConfirmed(payment.getOrderId());
		// The cart was deliberately left untouched at checkout time — this is the one place a
		// purchase is actually final, so this is where it gets cleared.
		cartService.clearCart(userId);

		eventPublisher.publish(KafkaTopics.PAYMENT_COMPLETED, payment.getId().toString(),
			new PaymentCompletedEvent(payment.getOrderId(), payment.getId(), userId, true, payment.getTransactionId()));

		return PaymentSummary.from(payment, razorpayProperties.getKeyId());
	}

	@Override
	@Transactional(readOnly = true)
	public PaymentSummary getPayment(UUID userId, UUID paymentId) {
		Payment payment = paymentRepository.findByIdAndUserId(paymentId, userId)
			.orElseThrow(() -> NotFoundException.payment(paymentId));
		return PaymentSummary.from(payment, razorpayProperties.getKeyId());
	}
}
