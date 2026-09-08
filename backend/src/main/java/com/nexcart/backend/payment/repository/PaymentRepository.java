package com.nexcart.backend.payment.repository;

import com.nexcart.backend.payment.domain.Payment;
import com.nexcart.backend.payment.domain.PaymentStatus;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {

	Optional<Payment> findByIdempotencyKey(String idempotencyKey);

	Optional<Payment> findByIdAndUserId(UUID id, UUID userId);

	// Used by initiate() to reuse an already-pending payment for this order (e.g. a second
	// browser tab) instead of creating a competing one — read while holding the Order row lock
	// (see OrderRepository.findByIdAndUserIdForUpdate), so no separate locking needed here.
	Optional<Payment> findByOrderIdAndStatus(UUID orderId, PaymentStatus status);

	// Locked variant for verify(): makes "check terminal state, then mutate" atomic against a
	// duplicate callback/retry racing itself for the same payment — the second caller blocks
	// until the first commits, then sees the already-terminal state and safely no-ops.
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("SELECT p FROM Payment p WHERE p.id = :id AND p.userId = :userId")
	Optional<Payment> findByIdAndUserIdForUpdate(@Param("id") UUID id, @Param("userId") UUID userId);
}
