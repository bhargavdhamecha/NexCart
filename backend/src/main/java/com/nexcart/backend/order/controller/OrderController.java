package com.nexcart.backend.order.controller;

import com.nexcart.backend.order.dto.OrderSummary;
import com.nexcart.backend.order.service.OrderService;
import com.nexcart.backend.security.jwt.AuthenticatedUser;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {

	private final OrderService orderService;

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public OrderSummary checkout(@AuthenticationPrincipal AuthenticatedUser user) {
		return orderService.checkout(user.id());
	}

	@GetMapping
	public List<OrderSummary> getOrders(@AuthenticationPrincipal AuthenticatedUser user) {
		return orderService.getOrders(user.id());
	}

	@GetMapping("/{id}")
	public OrderSummary getOrder(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID id) {
		return orderService.getOrder(user.id(), id);
	}
}
