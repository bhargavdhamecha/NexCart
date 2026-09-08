package com.nexcart.backend.payment.gateway;

import java.math.BigDecimal;

/**
 * Abstraction over the Razorpay SDK, so tests don't need a real Razorpay sandbox call (see
 * FakeRazorpayGatewayClientImpl, active under the "test" profile — mirrors the
 * StorageService/FakeStorageServiceImpl and EventPublisher/FakeEventPublisherImpl pattern used
 * elsewhere in this codebase).
 */
public interface RazorpayGatewayClient {

	GatewayOrder createOrder(BigDecimal amount, String currency, String receipt);

	/**
	 * Verifies that (razorpayPaymentId, razorpaySignature) genuinely correspond to
	 * razorpayOrderId, per Razorpay's own signed record of that order — NOT whatever the client
	 * claims. Callers must always pass an order id read back from their own storage (the one
	 * returned by createOrder() and persisted at that time), never one taken from the request
	 * body of the call being verified: trusting a client-submitted order id here would let a
	 * genuinely-valid signature from an attacker's own cheap transaction be replayed against a
	 * different, more expensive payment.
	 */
	boolean verifySignature(String razorpayOrderId, String razorpayPaymentId, String razorpaySignature);

	record GatewayOrder(String orderId) {
	}
}
