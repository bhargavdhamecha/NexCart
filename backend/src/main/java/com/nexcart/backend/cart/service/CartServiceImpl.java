package com.nexcart.backend.cart.service;

import com.nexcart.backend.cart.domain.Cart;
import com.nexcart.backend.cart.domain.CartItem;
import com.nexcart.backend.cart.dto.CartItemSummary;
import com.nexcart.backend.cart.dto.CartSummary;
import com.nexcart.backend.cart.repository.CartItemRepository;
import com.nexcart.backend.cart.repository.CartRepository;
import com.nexcart.backend.common.exception.ConflictException;
import com.nexcart.backend.common.exception.NotFoundException;
import com.nexcart.backend.product.domain.ProductStatus;
import com.nexcart.backend.product.dto.ProductSummary;
import com.nexcart.backend.product.service.ProductService;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CartServiceImpl implements CartService {

	private final CartRepository cartRepository;
	private final CartItemRepository cartItemRepository;
	private final ProductService productService;

	@Override
	@Transactional
	public CartSummary getCart(UUID userId) {
		return toSummary(getOrCreateCart(userId));
	}

	@Override
	@Transactional
	public CartSummary addItem(UUID userId, UUID productId, int quantity) {
		requireActiveProduct(productId);
		Cart cart = getOrCreateCart(userId);
		CartItem item = cartItemRepository.findByCartIdAndProductId(cart.getId(), productId)
			.orElse(null);
		if (item == null) {
			cartItemRepository.save(CartItem.create(cart, productId, quantity));
		}
		else {
			item.setQuantity(item.getQuantity() + quantity);
		}
		return toSummary(cart);
	}

	@Override
	@Transactional
	public CartSummary setItemQuantity(UUID userId, UUID productId, int quantity) {
		Cart cart = getOrCreateCart(userId);
		CartItem item = cartItemRepository.findByCartIdAndProductId(cart.getId(), productId)
			.orElseThrow(() -> NotFoundException.cartItem(productId));
		item.setQuantity(quantity);
		return toSummary(cart);
	}

	@Override
	@Transactional
	public CartSummary removeItem(UUID userId, UUID productId) {
		Cart cart = getOrCreateCart(userId);
		// Idempotent: removing something already absent is a no-op, not an error (standard
		// DELETE semantics).
		cartItemRepository.findByCartIdAndProductId(cart.getId(), productId)
			.ifPresent(cartItemRepository::delete);
		return toSummary(cart);
	}

	@Override
	@Transactional
	public CartSummary clearCart(UUID userId) {
		Cart cart = getOrCreateCart(userId);
		cartItemRepository.deleteByCartId(cart.getId());
		return toSummary(cart);
	}

	private void requireActiveProduct(UUID productId) {
		ProductSummary product = productService.getById(productId);
		if (product.status() != ProductStatus.ACTIVE) {
			throw ConflictException.productUnavailable(productId);
		}
	}

	private Cart getOrCreateCart(UUID userId) {
		return cartRepository.findByUserId(userId)
			.orElseGet(() -> cartRepository.save(Cart.create(userId)));
	}

	private CartSummary toSummary(Cart cart) {
		List<CartItem> items = cartItemRepository.findByCartIdOrderByCreatedAtAsc(cart.getId());
		BigDecimal totalAmount = BigDecimal.ZERO;
		int itemCount = 0;
		List<CartItemSummary> summaries = new ArrayList<>();

		for (CartItem item : items) {
			CartItemSummary summary = toItemSummary(item);
			summaries.add(summary);
			totalAmount = totalAmount.add(summary.lineTotal());
			itemCount += item.getQuantity();
		}

		return new CartSummary(cart.getId(), summaries, totalAmount, itemCount);
	}

	private CartItemSummary toItemSummary(CartItem item) {
		ProductSummary product;
		try {
			product = productService.getById(item.getProductId());
		}
		catch (NotFoundException exception) {
			return new CartItemSummary(item.getId(), item.getProductId(), "Product unavailable",
				BigDecimal.ZERO, null, item.getQuantity(), BigDecimal.ZERO, false);
		}

		boolean available = product.status() == ProductStatus.ACTIVE;
		BigDecimal price = available ? product.price() : BigDecimal.ZERO;
		BigDecimal lineTotal = price.multiply(BigDecimal.valueOf(item.getQuantity()));
		return new CartItemSummary(item.getId(), item.getProductId(), product.title(), price,
			product.primaryImageUrl(), item.getQuantity(), lineTotal, available);
	}
}
