package com.nexcart.gateway.config;

import java.net.InetSocketAddress;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Mono;

@Configuration
public class RateLimiterConfig {

	// Token-bucket limiter for the product API — 20 tokens/sec sustained, burst up to 40,
	// 1 token per request. Starting numbers, not load-tested; tune via the RedisRateLimiter
	// bean here if real traffic patterns call for it. Backed by Redis (token buckets live as
	// Lua-script-updated keys there) so the limit is consistent even if this gateway is scaled
	// to multiple instances later.
	@Bean
	RedisRateLimiter productRateLimiter() {
		return new RedisRateLimiter(20, 40, 1);
	}

	// The product listing/detail API is anonymous (SecurityConfig permits GET on it without
	// auth), so there's no user identity to key the limit on — falls back to the caller's IP.
	@Bean
	KeyResolver ipKeyResolver() {
		return exchange -> {
			InetSocketAddress remoteAddress = exchange.getRequest().getRemoteAddress();
			String ip = remoteAddress != null ? remoteAddress.getAddress().getHostAddress() : "unknown";
			return Mono.just(ip);
		};
	}
}
