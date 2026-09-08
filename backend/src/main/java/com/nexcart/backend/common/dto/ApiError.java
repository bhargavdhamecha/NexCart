package com.nexcart.backend.common.dto;

import com.nexcart.backend.common.exception.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import org.springframework.http.HttpStatus;

public record ApiError(
	Instant timestamp,
	int status,
	String code,
	String message,
	String path
) {

	public static ApiError of(HttpStatus status, ErrorCode errorCode, String message, HttpServletRequest request) {
		return of(status, errorCode.name(), message, request);
	}

	public static ApiError of(HttpStatus status, String code, String message, HttpServletRequest request) {
		return new ApiError(Instant.now(), status.value(), code, message, request.getRequestURI());
	}
}
