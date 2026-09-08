package com.nexcart.backend.payment.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.razorpay")
public class RazorpayProperties {

	/** Publishable — safe to hand to the frontend (used as Checkout's `key` option). */
	private String keyId;

	/** Never returned to the frontend — used only for signing/verifying server-side. */
	private String keySecret;
}
