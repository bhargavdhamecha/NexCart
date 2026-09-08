package com.nexcart.backend.inventory.service;

import com.nexcart.backend.common.exception.ConflictException;
import com.nexcart.backend.common.exception.NotFoundException;
import com.nexcart.backend.inventory.domain.Inventory;
import com.nexcart.backend.inventory.dto.InventorySummary;
import com.nexcart.backend.inventory.repository.InventoryRepository;
import jakarta.persistence.EntityManager;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class InventoryServiceImpl implements InventoryService {

	private final InventoryRepository inventoryRepository;
	private final EntityManager entityManager;

	@Override
	@Transactional
	public void initialize(UUID productId, int availableQuantity) {
		inventoryRepository.save(Inventory.initialize(productId, availableQuantity));
	}

	@Override
	@Transactional(readOnly = true)
	public Integer getAvailableQuantity(UUID productId) {
		return getByProductId(productId).getAvailableQuantity();
	}

	@Override
	@Transactional(readOnly = true)
	public Map<UUID, Integer> getAvailableQuantities(Collection<UUID> productIds) {
		if (productIds.isEmpty()) {
			return Map.of();
		}
		return inventoryRepository.findByProductIdIn(productIds).stream()
			.collect(Collectors.toMap(Inventory::getProductId, Inventory::getAvailableQuantity));
	}

	@Override
	@Transactional
	public InventorySummary adjustStock(UUID productId, int availableQuantity) {
		Inventory inventory = getByProductId(productId);
		inventory.setAvailableQuantity(availableQuantity);
		return InventorySummary.from(inventory);
	}

	@Override
	@Transactional
	public void decrease(UUID productId, int quantity) {
		// Pessimistic write lock: a second concurrent checkout for the same product blocks here
		// until this transaction commits/rolls back, so "stock = 1, two simultaneous buyers"
		// can never both succeed — the loser sees the post-decrement quantity once unblocked.
		Inventory inventory = getByProductIdForUpdate(productId);
		if (inventory.getAvailableQuantity() < quantity) {
			throw ConflictException.insufficientStock(productId);
		}
		inventory.setAvailableQuantity(inventory.getAvailableQuantity() - quantity);
	}

	@Override
	@Transactional
	public void increase(UUID productId, int quantity) {
		Inventory inventory = getByProductIdForUpdate(productId);
		inventory.setAvailableQuantity(inventory.getAvailableQuantity() + quantity);
	}

	private Inventory getByProductId(UUID productId) {
		return inventoryRepository.findByProductId(productId)
			.orElseThrow(() -> NotFoundException.inventory(productId));
	}

	private Inventory getByProductIdForUpdate(UUID productId) {
		Inventory inventory = inventoryRepository.findByProductIdForUpdate(productId)
			.orElseThrow(() -> NotFoundException.inventory(productId));
		// findByProductIdForUpdate() genuinely issues "... FOR UPDATE" and genuinely acquires the
		// DB row lock — but if this same Inventory row was already loaded earlier in this same
		// transaction via an *unlocked* read (checkout() calls ProductService.getById() for
		// price/status revalidation first, which internally touches
		// InventoryService.getAvailableQuantity()), Hibernate's first-level cache hands back
		// that same already-managed Java object instead of re-hydrating it from this query's
		// result set — so the lock is real at the DB level, but the in-memory value the caller
		// then checks can still be stale, silently defeating the lock. Confirmed by reproducing
		// a real oversell against live Postgres (two concurrent checkouts both succeeding on a
		// stock=1 item) before adding this refresh. Forces this specific entity back in sync
		// with the row this method just locked.
		entityManager.refresh(inventory);
		return inventory;
	}
}
