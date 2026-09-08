package com.nexcart.backend.product.service;

import com.nexcart.backend.common.dto.PageResponse;
import com.nexcart.backend.product.dto.ProductCreateRequest;
import com.nexcart.backend.product.dto.ProductSummary;
import com.nexcart.backend.product.dto.ProductUpdateRequest;
import java.util.UUID;
import org.springframework.data.domain.Pageable;

public interface ProductService {

	PageResponse<ProductSummary> list(UUID categoryId, String search, Pageable pageable);

	/** Not filtered by status, so an admin can still inspect an INACTIVE (soft-deleted) product. */
	ProductSummary getById(UUID id);

	ProductSummary create(ProductCreateRequest request);

	ProductSummary update(UUID id, ProductUpdateRequest request);

	/** Soft delete: sets status = INACTIVE rather than removing the row. */
	void delete(UUID id);
}
