package com.nexcart.backend.security.config;

import java.time.Duration;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.security")
public class SecurityProperties {

	private String jwtSecret;
	private Duration accessTokenTtl = Duration.ofMinutes(15);
	private Duration refreshTokenTtl = Duration.ofDays(30);
	private String refreshCookieName = "nexcart_refresh_token";
	private String refreshCookiePath = "/api/v1/auth";
	private boolean refreshCookieSecure = true;
	private List<String> corsAllowedOrigins = List.of("http://localhost:4200");
}
