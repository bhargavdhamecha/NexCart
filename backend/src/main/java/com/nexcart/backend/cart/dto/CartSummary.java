package com.nexcart.backend.cart.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record CartSummary(
	UUID cartId,
	List<CartItemSummary> items,
	BigDecimal totalAmount,
	int itemCount
) {
}
