package com.nexcart.backend.user.dto;

public record UserAuthResponse(
	UserSummary user,
	String accessToken,
	String tokenType,
	long expiresIn
) {

	public static UserAuthResponse of(UserSummary user, String accessToken, long expiresIn) {
		return new UserAuthResponse(user, accessToken, "Bearer", expiresIn);
	}
}
