package com.nexcart.backend.payment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record PaymentInitiateRequest(
	@NotNull(message = "Order is required")
	UUID orderId,

	@NotBlank(message = "Idempotency key is required")
	String idempotencyKey,

	@NotBlank(message = "Payment method is required")
	String paymentMethod
) {
}
