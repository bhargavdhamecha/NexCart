package com.nexcart.backend.order.domain;

public enum OrderStatus {
	CREATED,
	PENDING_PAYMENT,
	PAID,
	CONFIRMED,
	PAYMENT_FAILED,
	CANCELLED
}
