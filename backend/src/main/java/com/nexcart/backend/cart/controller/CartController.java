package com.nexcart.backend.cart.controller;

import com.nexcart.backend.cart.dto.AddCartItemRequest;
import com.nexcart.backend.cart.dto.CartSummary;
import com.nexcart.backend.cart.dto.UpdateCartItemRequest;
import com.nexcart.backend.cart.service.CartService;
import com.nexcart.backend.security.jwt.AuthenticatedUser;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// No SecurityConfig changes needed — everything not explicitly permitAll already requires
// authentication, and nothing under /api/v1/cart is in that permitAll list.
@RestController
@RequestMapping("/api/v1/cart")
@RequiredArgsConstructor
public class CartController {

	private final CartService cartService;

	@GetMapping
	public CartSummary getCart(@AuthenticationPrincipal AuthenticatedUser user) {
		return cartService.getCart(user.id());
	}

	@PostMapping("/items")
	public CartSummary addItem(@AuthenticationPrincipal AuthenticatedUser user,
		@Valid @RequestBody AddCartItemRequest request) {
		return cartService.addItem(user.id(), request.productId(), request.quantity());
	}

	@PutMapping("/items/{productId}")
	public CartSummary setItemQuantity(@AuthenticationPrincipal AuthenticatedUser user,
		@PathVariable UUID productId, @Valid @RequestBody UpdateCartItemRequest request) {
		return cartService.setItemQuantity(user.id(), productId, request.quantity());
	}

	@DeleteMapping("/items/{productId}")
	public CartSummary removeItem(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID productId) {
		return cartService.removeItem(user.id(), productId);
	}

	@DeleteMapping
	public CartSummary clearCart(@AuthenticationPrincipal AuthenticatedUser user) {
		return cartService.clearCart(user.id());
	}
}
