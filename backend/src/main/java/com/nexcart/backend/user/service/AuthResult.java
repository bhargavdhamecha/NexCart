package com.nexcart.backend.user.service;

import com.nexcart.backend.user.dto.UserSummary;

public record AuthResult(
	UserSummary user,
	String accessToken,
	long expiresIn,
	String refreshToken
) {
}
