package com.nexcart.backend.cart.service;

import com.nexcart.backend.cart.dto.CartSummary;
import java.util.UUID;

public interface CartService {

	CartSummary getCart(UUID userId);

	CartSummary addItem(UUID userId, UUID productId, int quantity);

	CartSummary setItemQuantity(UUID userId, UUID productId, int quantity);

	CartSummary removeItem(UUID userId, UUID productId);

	CartSummary clearCart(UUID userId);
}
