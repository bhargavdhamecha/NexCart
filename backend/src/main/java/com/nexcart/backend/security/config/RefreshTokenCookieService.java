package com.nexcart.backend.security.config;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Duration;
import java.util.Arrays;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
public class RefreshTokenCookieService {

	private final SecurityProperties securityProperties;

	public void addRefreshTokenCookie(HttpServletResponse response, String refreshToken) {
		ResponseCookie cookie = buildCookie(refreshToken, securityProperties.getRefreshTokenTtl());
		response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
	}

	public void clearRefreshTokenCookie(HttpServletResponse response) {
		ResponseCookie cookie = buildCookie("", Duration.ZERO);
		response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
	}

	private ResponseCookie buildCookie(String value, Duration maxAge) {
		return ResponseCookie.from(securityProperties.getRefreshCookieName(), value)
			.httpOnly(true)
			.secure(securityProperties.isRefreshCookieSecure())
			.sameSite("Strict")
			.path(securityProperties.getRefreshCookiePath())
			.maxAge(maxAge)
			.build();
	}

	public Optional<String> extractRefreshToken(HttpServletRequest request) {
		if (request.getCookies() == null) {
			return Optional.empty();
		}

		return Arrays.stream(request.getCookies())
			.filter(cookie -> securityProperties.getRefreshCookieName().equals(cookie.getName()))
			.map(Cookie::getValue)
			.filter(StringUtils::hasText)
			.findFirst();
	}
}
