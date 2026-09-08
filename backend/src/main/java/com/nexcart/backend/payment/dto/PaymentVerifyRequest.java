package com.nexcart.backend.payment.dto;

import jakarta.validation.constraints.NotBlank;

// Deliberately no razorpayOrderId field — verification always uses the order id this server
// stored at initiate() time, never one the client submits (see RazorpayGatewayClient javadoc).
public record PaymentVerifyRequest(
	@NotBlank(message = "Razorpay payment id is required")
	String razorpayPaymentId,

	@NotBlank(message = "Razorpay signature is required")
	String razorpaySignature
) {
}
