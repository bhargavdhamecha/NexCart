package com.nexcart.backend.order.dto;

import com.nexcart.backend.order.domain.Order;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record OrderSummary(
	UUID orderId,
	BigDecimal totalAmount,
	Instant orderDate,
	String status,
	List<OrderItemSummary> items
) {

	public static OrderSummary from(Order order, List<OrderItemSummary> items) {
		return new OrderSummary(order.getId(), order.getTotalAmount(), order.getCreatedAt(),
			order.getStatus().name(), items);
	}
}
