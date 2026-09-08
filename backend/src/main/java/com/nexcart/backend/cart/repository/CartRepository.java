package com.nexcart.backend.cart.repository;

import com.nexcart.backend.cart.domain.Cart;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CartRepository extends JpaRepository<Cart, UUID> {

	Optional<Cart> findByUserId(UUID userId);
}
