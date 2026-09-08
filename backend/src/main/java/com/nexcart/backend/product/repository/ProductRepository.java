package com.nexcart.backend.product.repository;

import com.nexcart.backend.product.domain.Product;
import com.nexcart.backend.product.domain.ProductStatus;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductRepository extends JpaRepository<Product, UUID> {

	// The only JPQL query in the codebase: three independent optional filters (status is
	// always applied, categoryId/search are optional) can't be expressed cleanly via derived
	// method names without an explosion of method variants. Still deliberately simple/no
	// Specification API and no full-text search engine, per context.md's guidance to prefer
	// simple architecture and avoid Elasticsearch early.
	//
	// searchPattern is the caller's already-lowercased, "%"-wrapped value (see
	// ProductServiceImpl) rather than building it here with LOWER(CONCAT('%', :search, '%')) —
	// Postgres can't infer a type for CONCAT's :search argument when it's bound null (the
	// common short-circuit case), and defaults it to bytea, breaking LOWER(bytea).
	@Query("""
		SELECT p FROM Product p
		WHERE p.status = :status
		  AND (:categoryId IS NULL OR p.category.id = :categoryId)
		  AND (:searchPattern IS NULL OR LOWER(p.title) LIKE :searchPattern)
		""")
	Page<Product> search(@Param("status") ProductStatus status, @Param("categoryId") UUID categoryId,
		@Param("searchPattern") String searchPattern, Pageable pageable);
}
