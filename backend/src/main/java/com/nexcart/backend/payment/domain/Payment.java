package com.nexcart.backend.payment.domain;

import com.nexcart.backend.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "payments", uniqueConstraints = {
	@UniqueConstraint(name = "uk_payments_idempotency_key", columnNames = "idempotency_key")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Payment extends BaseEntity {

	@Column(name = "order_id", nullable = false)
	private UUID orderId;

	@Column(name = "user_id", nullable = false)
	private UUID userId;

	@Column(nullable = false, precision = 12, scale = 2)
	private BigDecimal amount;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	@Setter
	private PaymentStatus status;

	// Nullable — set only once Razorpay's signature on the payment has been verified.
	@Column(length = 100)
	@Setter
	private String transactionId;

	// Nullable — set right after createOrder() succeeds in initiate(). Verification always
	// reads this stored value, never a client-submitted one (see RazorpayGatewayClient javadoc).
	@Column(name = "razorpay_order_id", length = 100)
	@Setter
	private String razorpayOrderId;

	@Column(name = "idempotency_key", nullable = false, length = 100)
	private String idempotencyKey;

	@Column(name = "payment_method", nullable = false, length = 50)
	private String paymentMethod;

	public static Payment create(UUID orderId, UUID userId, BigDecimal amount, String idempotencyKey,
		String paymentMethod) {
		Payment payment = new Payment();
		payment.orderId = orderId;
		payment.userId = userId;
		payment.amount = amount;
		payment.status = PaymentStatus.INITIATED;
		payment.idempotencyKey = idempotencyKey;
		payment.paymentMethod = paymentMethod;
		return payment;
	}
}
