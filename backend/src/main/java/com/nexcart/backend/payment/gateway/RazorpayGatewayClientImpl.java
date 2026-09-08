package com.nexcart.backend.payment.gateway;

import com.nexcart.backend.common.exception.ApiException;
import com.nexcart.backend.common.exception.ErrorCode;
import com.nexcart.backend.payment.config.RazorpayProperties;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.razorpay.Utils;
import java.math.BigDecimal;
import java.math.RoundingMode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
@Profile("!test")
@RequiredArgsConstructor
@Slf4j
public class RazorpayGatewayClientImpl implements RazorpayGatewayClient {

	private final RazorpayClient razorpayClient;
	private final RazorpayProperties razorpayProperties;

	@Override
	public GatewayOrder createOrder(BigDecimal amount, String currency, String receipt) {
		requireConfigured();
		try {
			JSONObject request = new JSONObject();
			// Razorpay amounts are in the smallest currency unit — paise, not rupees.
			request.put("amount", amount.multiply(BigDecimal.valueOf(100)).setScale(0, RoundingMode.HALF_UP)
				.intValueExact());
			request.put("currency", currency);
			request.put("receipt", receipt);

			// Fully-qualified: com.razorpay.Order, not this codebase's own order.domain.Order.
			com.razorpay.Order order = razorpayClient.orders.create(request);
			return new GatewayOrder(order.get("id"));
		}
		catch (RazorpayException exception) {
			// The exception message can carry Razorpay/SDK-internal detail — log it for
			// diagnosis, but keep what reaches the (customer-facing) API response generic.
			log.error("Failed to create Razorpay order for receipt {}", receipt, exception);
			throw new ApiException(HttpStatus.BAD_GATEWAY, ErrorCode.PAYMENT_GATEWAY_ERROR,
				"Could not start payment. Please try again.");
		}
	}

	@Override
	public boolean verifySignature(String razorpayOrderId, String razorpayPaymentId, String razorpaySignature) {
		try {
			JSONObject attributes = new JSONObject();
			attributes.put("razorpay_order_id", razorpayOrderId);
			attributes.put("razorpay_payment_id", razorpayPaymentId);
			attributes.put("razorpay_signature", razorpaySignature);
			return Utils.verifyPaymentSignature(attributes, razorpayProperties.getKeySecret());
		}
		catch (RazorpayException exception) {
			// Malformed/missing fields, or a genuinely wrong signature — either way, not verified.
			return false;
		}
	}

	private void requireConfigured() {
		if (isBlank(razorpayProperties.getKeyId()) || isBlank(razorpayProperties.getKeySecret())) {
			// Unlike B2 storage (an admin-only upload path), this one is reachable by any
			// shopper at checkout — the response (and the app-wide error toast built from it)
			// must not leak env var names to them. The detailed hint stays server-side, in logs.
			log.warn("Payment attempted with Razorpay not configured — set RAZORPAY_KEY_ID/RAZORPAY_KEY_SECRET");
			throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, ErrorCode.PAYMENT_GATEWAY_NOT_CONFIGURED,
				"Online payment isn't available right now. Please try again later.");
		}
	}

	private boolean isBlank(String value) {
		return value == null || value.isBlank();
	}
}
