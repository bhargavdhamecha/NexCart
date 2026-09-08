package com.nexcart.backend.order.repository;

import com.nexcart.backend.order.domain.OrderItem;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderItemRepository extends JpaRepository<OrderItem, UUID> {

	List<OrderItem> findByOrderIdOrderByCreatedAtAsc(UUID orderId);
}
