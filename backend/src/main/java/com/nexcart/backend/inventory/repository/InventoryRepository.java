package com.nexcart.backend.inventory.repository;

import com.nexcart.backend.inventory.domain.Inventory;
import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InventoryRepository extends JpaRepository<Inventory, UUID> {

	Optional<Inventory> findByProductId(UUID productId);

	// Batched, unlocked read for listing/detail pages — one query for a whole page of products
	// instead of one per product. Never used for a decrement/increment (see the locked variant
	// below for that).
	List<Inventory> findByProductIdIn(Collection<UUID> productIds);

	// SELECT ... FOR UPDATE — a second concurrent checkout for the same product blocks here
	// until the first transaction commits/rolls back, then reads the up-to-date row. This is
	// what actually prevents overselling the last unit (the plain findByProductId above is not
	// safe for that — see InventoryService.decrease/increase).
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("SELECT i FROM Inventory i WHERE i.productId = :productId")
	Optional<Inventory> findByProductIdForUpdate(@Param("productId") UUID productId);
}
