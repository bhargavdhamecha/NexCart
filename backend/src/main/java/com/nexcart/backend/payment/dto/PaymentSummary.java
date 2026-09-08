package com.nexcart.backend.payment.dto;

import com.nexcart.backend.payment.domain.Payment;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PaymentSummary(
	UUID paymentId,
	UUID orderId,
	BigDecimal amount,
	String currency,
	String status,
	String transactionId,
	String paymentMethod,
	String razorpayOrderId,
	// Publishable — safe to hand to the frontend, used as Checkout's `key` option.
	String razorpayKeyId,
	Instant createdAt,
	Instant updatedAt
) {

	public static PaymentSummary from(Payment payment, String razorpayKeyId) {
		return new PaymentSummary(payment.getId(), payment.getOrderId(), payment.getAmount(), "INR",
			payment.getStatus().name(), payment.getTransactionId(), payment.getPaymentMethod(),
			payment.getRazorpayOrderId(), razorpayKeyId, payment.getCreatedAt(), payment.getUpdatedAt());
	}
}
