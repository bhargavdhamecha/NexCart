package com.nexcart.backend.common.exception;

import org.springframework.http.HttpStatus;

public class BadRequestException extends ApiException {

	public BadRequestException(ErrorCode errorCode, String message) {
		super(HttpStatus.BAD_REQUEST, errorCode, message);
	}

	public static BadRequestException invalidImageFile(String reason) {
		return new BadRequestException(ErrorCode.INVALID_IMAGE_FILE, reason);
	}

	public static BadRequestException tooManyImages(int max) {
		return new BadRequestException(ErrorCode.TOO_MANY_IMAGES,
			"A product can have at most " + max + " images");
	}

	public static BadRequestException emptyCart() {
		return new BadRequestException(ErrorCode.CART_EMPTY, "Cannot checkout an empty cart");
	}
}
