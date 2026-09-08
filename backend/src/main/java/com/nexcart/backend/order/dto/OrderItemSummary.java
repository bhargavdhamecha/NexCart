package com.nexcart.backend.order.dto;

import com.nexcart.backend.order.domain.OrderItem;
import java.math.BigDecimal;
import java.util.UUID;

public record OrderItemSummary(
	UUID orderItemId,
	UUID productId,
	String title,
	Integer quantity,
	BigDecimal unitPrice,
	BigDecimal itemTotal,
	String imageUrl
) {

	public static OrderItemSummary from(OrderItem item) {
		return new OrderItemSummary(item.getId(), item.getProductId(), item.getProductTitle(),
			item.getQuantity(), item.getUnitPrice(), item.getItemTotal(), item.getProductImageUrl());
	}
}
