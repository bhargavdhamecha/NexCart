package com.nexcart.backend.common.exception;

import java.util.UUID;
import org.springframework.http.HttpStatus;

public class ConflictException extends ApiException {

	public ConflictException(ErrorCode errorCode, String message) {
		super(HttpStatus.CONFLICT, errorCode, message);
	}

	public static ConflictException insufficientStock(UUID productId) {
		return new ConflictException(ErrorCode.INSUFFICIENT_STOCK, "Not enough stock for product: " + productId);
	}

	public static ConflictException productUnavailable(UUID productId) {
		return new ConflictException(ErrorCode.PRODUCT_UNAVAILABLE, "Product is no longer available: " + productId);
	}

	public static ConflictException invalidOrderState(String message) {
		return new ConflictException(ErrorCode.INVALID_ORDER_STATE, message);
	}
}
