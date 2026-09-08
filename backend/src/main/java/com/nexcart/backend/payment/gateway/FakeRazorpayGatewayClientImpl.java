package com.nexcart.backend.payment.gateway;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import java.util.concurrent.atomic.AtomicInteger;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

/**
 * Test-only stand-in for RazorpayGatewayClientImpl — no real Razorpay network call, deterministic
 * signatures instead. Mirrors FakeEventPublisherImpl/FakeStorageServiceImpl.
 */
@Service
@Profile("test")
public class FakeRazorpayGatewayClientImpl implements RazorpayGatewayClient {

	/** Fixed "secret" used to compute/verify signatures in tests — never a real Razorpay key. */
	public static final String TEST_KEY_SECRET = "fake-test-secret-for-deterministic-signatures";

	private final AtomicInteger counter = new AtomicInteger();

	@Override
	public GatewayOrder createOrder(BigDecimal amount, String currency, String receipt) {
		return new GatewayOrder("order_fake_" + counter.incrementAndGet());
	}

	@Override
	public boolean verifySignature(String razorpayOrderId, String razorpayPaymentId, String razorpaySignature) {
		return computeSignature(razorpayOrderId, razorpayPaymentId).equals(razorpaySignature);
	}

	/**
	 * Reimplements Razorpay's own signature formula — HMAC_SHA256(order_id + "|" + payment_id,
	 * secret) — against the fixed TEST_KEY_SECRET, so integration tests can construct a
	 * genuinely-valid signature (and, by mutating a character, a deliberately-tampered one)
	 * without any real Razorpay call.
	 */
	public static String computeSignature(String razorpayOrderId, String razorpayPaymentId) {
		try {
			Mac mac = Mac.getInstance("HmacSHA256");
			mac.init(new SecretKeySpec(TEST_KEY_SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
			byte[] hash = mac.doFinal((razorpayOrderId + "|" + razorpayPaymentId).getBytes(StandardCharsets.UTF_8));
			return HexFormat.of().formatHex(hash);
		}
		catch (Exception exception) {
			throw new IllegalStateException(exception);
		}
	}
}
