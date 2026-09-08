package com.nexcart.backend.payment.config;

import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Builds the RazorpayClient used to talk to Razorpay's API. Always constructs successfully even
 * without real credentials configured (RazorpayClient's constructor doesn't validate its
 * arguments) — RazorpayGatewayClientImpl.requireConfigured() is what actually fails clearly, the
 * first time a payment is initiated, not this bean's construction.
 */
@Configuration
@EnableConfigurationProperties(RazorpayProperties.class)
@RequiredArgsConstructor
public class RazorpayConfig {

	private final RazorpayProperties razorpayProperties;

	@Bean
	RazorpayClient razorpayClient() throws RazorpayException {
		return new RazorpayClient(razorpayProperties.getKeyId(), razorpayProperties.getKeySecret());
	}
}
