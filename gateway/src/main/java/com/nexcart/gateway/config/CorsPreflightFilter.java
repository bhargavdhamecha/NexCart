package com.nexcart.gateway.config;

import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

/**
 * Answers CORS preflight (OPTIONS) requests directly, rather than routing them through Spring
 * Cloud Gateway. This is a workaround, not the "intended" mechanism — the documented approach
 * (spring.cloud.gateway.globalcors, with add-to-simple-url-handler-mapping=true so WebFlux's own
 * preflight short-circuit picks it up) was tried first and, in this Spring Cloud Gateway 5.0.x /
 * Spring Boot 4.1.1 combination, consistently produced a bare 200 with none of the required
 * Access-Control-* headers — confirmed live, in both .properties and YAML form. Handling
 * preflight explicitly here sidesteps whatever's not wiring up correctly in that mechanism for
 * this version combination, and is arguably the more standard gateway pattern regardless: answer
 * preflight at the edge, don't propagate it downstream. Real (non-OPTIONS) requests are
 * unaffected by this filter and proceed to RouteConfig's routes as before.
 */
@Component
public class CorsPreflightFilter implements WebFilter, Ordered {

	private static final String ALLOWED_ORIGIN = "http://localhost:4200";

	@Override
	public int getOrder() {
		return Ordered.HIGHEST_PRECEDENCE;
	}

	@Override
	public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
		ServerHttpResponse response = exchange.getResponse();
		HttpHeaders headers = response.getHeaders();
		headers.add("Access-Control-Allow-Origin", ALLOWED_ORIGIN);
		headers.add("Access-Control-Allow-Credentials", "true");

		if (exchange.getRequest().getMethod() == HttpMethod.OPTIONS) {
			headers.add("Access-Control-Allow-Methods", "GET,POST,PUT,PATCH,DELETE,OPTIONS");
			// A literal "*" is invalid here per the CORS spec once Allow-Credentials is true —
			// browsers silently treat it as "no headers allowed" rather than "any header",
			// which is exactly what broke the real request next (Content-Type rejected).
			// Echoing back whatever the browser actually asked for in this preflight is the
			// standard, correct pattern instead of maintaining a static allow-list here.
			String requestedHeaders = exchange.getRequest().getHeaders()
				.getFirst("Access-Control-Request-Headers");
			headers.add("Access-Control-Allow-Headers", requestedHeaders != null ? requestedHeaders : "*");
			response.setStatusCode(HttpStatus.OK);
			return Mono.empty();
		}

		return chain.filter(exchange);
	}
}
