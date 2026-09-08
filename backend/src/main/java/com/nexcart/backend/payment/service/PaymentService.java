package com.nexcart.backend.payment.service;

import com.nexcart.backend.payment.dto.PaymentInitiateRequest;
import com.nexcart.backend.payment.dto.PaymentSummary;
import com.nexcart.backend.payment.dto.PaymentVerifyRequest;
import java.util.UUID;

public interface PaymentService {

	/**
	 * Idempotent on idempotencyKey: a repeat call with the same key returns the existing
	 * Payment unchanged rather than creating a duplicate (context.md: "idempotent payment
	 * processing... duplicate request"). Also creates a Razorpay order for this payment and
	 * stores its id, ready for the frontend to open Checkout with.
	 */
	PaymentSummary initiate(UUID userId, PaymentInitiateRequest request);

	/**
	 * Verifies Razorpay's signature on the payment before persisting anything — the whole point
	 * of this method is that success is never client-asserted, only cryptographically confirmed
	 * server-side. Idempotent on terminal state: verifying an already-SUCCESS/FAILED payment
	 * again just returns its current state, with no side effects re-applied. An invalid
	 * signature is rejected (400) without touching any state at all.
	 */
	PaymentSummary verify(UUID userId, UUID paymentId, PaymentVerifyRequest request);

	PaymentSummary getPayment(UUID userId, UUID paymentId);
}
