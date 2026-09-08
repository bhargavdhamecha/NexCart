package com.nexcart.backend.common.exception;

import org.springframework.http.HttpStatus;

public class AuthException extends ApiException {

	public AuthException(HttpStatus status, ErrorCode errorCode, String message) {
		super(status, errorCode, message);
	}

	public static AuthException invalidCredentials() {
		return new AuthException(HttpStatus.UNAUTHORIZED, ErrorCode.AUTH_INVALID_CREDENTIALS,
			"Invalid email or password");
	}

	public static AuthException invalidAccessToken() {
		return new AuthException(HttpStatus.UNAUTHORIZED, ErrorCode.AUTH_INVALID_ACCESS_TOKEN,
			"Invalid access token");
	}

	public static AuthException invalidSession() {
		return new AuthException(HttpStatus.UNAUTHORIZED, ErrorCode.AUTH_INVALID_SESSION,
			"Invalid authentication session");
	}

	public static AuthException accountDisabled() {
		return new AuthException(HttpStatus.FORBIDDEN, ErrorCode.AUTH_ACCOUNT_DISABLED,
			"Account is disabled");
	}
}
