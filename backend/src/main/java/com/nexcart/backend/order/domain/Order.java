package com.nexcart.backend.order.domain;

import com.nexcart.backend.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "orders")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Order extends BaseEntity {

	// Optimistic lock: two concurrent status transitions on the same order (e.g. a duplicate
	// payment-completion webhook racing itself) can't both silently win — the second writer's
	// flush fails with ObjectOptimisticLockingFailureException (mapped to 409 by
	// GlobalExceptionHandler) once it sees the version has already moved on.
	@Version
	@Column(nullable = false)
	private Long version;

	@Column(name = "user_id", nullable = false)
	private UUID userId;

	@Column(nullable = false, precision = 12, scale = 2)
	private BigDecimal totalAmount;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	@Setter
	private OrderStatus status;

	// Nullable — set once a payment is initiated for this order (see PaymentServiceImpl).
	@Column(name = "payment_id")
	@Setter
	private UUID paymentId;

	public static Order create(UUID userId, BigDecimal totalAmount) {
		Order order = new Order();
		order.userId = userId;
		order.totalAmount = totalAmount;
		order.status = OrderStatus.CREATED;
		return order;
	}
}
