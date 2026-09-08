package com.nexcart.backend.order.repository;

import com.nexcart.backend.order.domain.Order;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrderRepository extends JpaRepository<Order, UUID> {

	List<Order> findByUserIdOrderByCreatedAtDesc(UUID userId);

	// Ownership-scoped lookup — a non-owner gets 404 (via NotFoundException.order), not 403, so
	// a guessed order id never confirms whether it exists.
	Optional<Order> findByIdAndUserId(UUID id, UUID userId);

	// Locked variant for PaymentServiceImpl.initiate(): makes "check for an already-pending
	// Payment on this order, else create one" atomic against a second initiate() call for the
	// same order racing it (e.g. two browser tabs) — the second caller blocks until the first
	// commits, then sees the just-created Payment instead of creating a competing one.
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("SELECT o FROM Order o WHERE o.id = :id AND o.userId = :userId")
	Optional<Order> findByIdAndUserIdForUpdate(@Param("id") UUID id, @Param("userId") UUID userId);
}
