package com.nexcart.backend.user.service;

import jakarta.servlet.http.HttpServletRequest;

public record RequestMetadata(
	String userAgent,
	String ipAddress
) {

	public static RequestMetadata from(HttpServletRequest request) {
		String forwardedFor = request.getHeader("X-Forwarded-For");
		String ipAddress = forwardedFor != null && !forwardedFor.isBlank()
			? forwardedFor.split(",")[0].trim()
			: request.getRemoteAddr();
		return new RequestMetadata(request.getHeader("User-Agent"), ipAddress);
	}
}
