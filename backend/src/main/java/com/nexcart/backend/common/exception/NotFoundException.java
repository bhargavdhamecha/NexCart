package com.nexcart.backend.common.exception;

import java.util.UUID;
import org.springframework.http.HttpStatus;

public class NotFoundException extends ApiException {

	public NotFoundException(ErrorCode errorCode, String message) {
		super(HttpStatus.NOT_FOUND, errorCode, message);
	}

	public static NotFoundException product(UUID id) {
		return new NotFoundException(ErrorCode.PRODUCT_NOT_FOUND, "Product not found: " + id);
	}

	public static NotFoundException category(UUID id) {
		return new NotFoundException(ErrorCode.CATEGORY_NOT_FOUND, "Category not found: " + id);
	}

	public static NotFoundException inventory(UUID productId) {
		return new NotFoundException(ErrorCode.INVENTORY_NOT_FOUND, "Inventory not found for product: " + productId);
	}

	public static NotFoundException productImage(UUID id) {
		return new NotFoundException(ErrorCode.PRODUCT_IMAGE_NOT_FOUND, "Product image not found: " + id);
	}

	public static NotFoundException cartItem(UUID id) {
		return new NotFoundException(ErrorCode.CART_ITEM_NOT_FOUND, "Cart item not found: " + id);
	}

	public static NotFoundException order(UUID id) {
		return new NotFoundException(ErrorCode.ORDER_NOT_FOUND, "Order not found: " + id);
	}

	public static NotFoundException payment(UUID id) {
		return new NotFoundException(ErrorCode.PAYMENT_NOT_FOUND, "Payment not found: " + id);
	}
}
