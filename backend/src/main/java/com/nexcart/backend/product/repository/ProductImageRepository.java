package com.nexcart.backend.product.repository;

import com.nexcart.backend.product.domain.ProductImage;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductImageRepository extends JpaRepository<ProductImage, UUID> {

	List<ProductImage> findByProductIdOrderByDisplayOrderAsc(UUID productId);

	// Batched variant for building a page of products at once — avoids one query per product
	// (see ProductCatalogCacheImpl).
	List<ProductImage> findByProductIdInOrderByDisplayOrderAsc(Collection<UUID> productIds);

	long countByProductId(UUID productId);

	Optional<ProductImage> findByIdAndProductId(UUID id, UUID productId);
}
