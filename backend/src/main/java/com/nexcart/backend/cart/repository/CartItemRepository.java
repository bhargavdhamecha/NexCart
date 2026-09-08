package com.nexcart.backend.cart.repository;

import com.nexcart.backend.cart.domain.CartItem;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CartItemRepository extends JpaRepository<CartItem, UUID> {

	List<CartItem> findByCartIdOrderByCreatedAtAsc(UUID cartId);

	Optional<CartItem> findByCartIdAndProductId(UUID cartId, UUID productId);

	void deleteByCartId(UUID cartId);
}
