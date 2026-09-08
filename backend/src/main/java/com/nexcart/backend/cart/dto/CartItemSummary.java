package com.nexcart.backend.cart.dto;

import java.math.BigDecimal;
import java.util.UUID;

// Built in CartServiceImpl by joining a CartItem with live product data — title/price/imageUrl/
// available are never persisted here (context.md: cart is "purchase intention only").
public record CartItemSummary(
	UUID cartItemId,
	UUID productId,
	String title,
	BigDecimal price,
	String imageUrl,
	Integer quantity,
	BigDecimal lineTotal,
	boolean available
) {
}
