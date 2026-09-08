package com.nexcart.gateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.GatewayFilterSpec;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Every /api/v1/** request is proxied to the backend — the frontend only needs one base URL
 * (this gateway's) rather than juggling two. Only the product routes get the rate-limiter
 * filter attached; everything else (auth, cart, orders, payments, categories) passes through
 * unrated. Route declaration order matters here: the more specific /products/** route must be
 * added before the general /api/v1/** one, since RouteLocatorBuilder evaluates routes in the
 * order they were added.
 *
 * Both routes carry dedupeResponseHeader on the CORS headers — the backend already sets
 * Access-Control-Allow-Origin/-Credentials (SecurityConfig.corsConfigurationSource()), and this
 * gateway adds the same two again on top regardless (confirmed even on a 404 for a totally
 * unmatched path, so it's baseline Spring Cloud Gateway/WebFlux behavior, not anything this app
 * configured) — the browser then rejects the response outright for carrying the header twice.
 * This is Spring Cloud Gateway's own documented filter for exactly this scenario (a downstream
 * that already sets its own CORS headers).
 */
@Configuration
public class RouteConfig {

	private static final String DEDUPE_CORS_HEADERS = "Access-Control-Allow-Origin Access-Control-Allow-Credentials";

	@Value("${app.gateway.backend-uri}")
	private String backendUri;

	@Bean
	RouteLocator routes(RouteLocatorBuilder builder, RedisRateLimiter productRateLimiter,
		KeyResolver ipKeyResolver) {
		return builder.routes()
			.route("product-api-rate-limited", route -> route
				.path("/api/v1/products/**")
				.filters(f -> f
					.requestRateLimiter(config -> config
						.setRateLimiter(productRateLimiter)
						.setKeyResolver(ipKeyResolver))
					.dedupeResponseHeader(DEDUPE_CORS_HEADERS, "RETAIN_UNIQUE"))
				.uri(backendUri))
			.route("backend-passthrough", route -> route
				.path("/api/v1/**")
				.filters(f -> f.dedupeResponseHeader(DEDUPE_CORS_HEADERS, "RETAIN_UNIQUE"))
				.uri(backendUri))
			.build();
	}
}
