package com.nexcart.backend.product.controller;

import com.nexcart.backend.common.dto.PageResponse;
import com.nexcart.backend.product.dto.ProductCreateRequest;
import com.nexcart.backend.product.dto.ProductSummary;
import com.nexcart.backend.product.dto.ProductUpdateRequest;
import com.nexcart.backend.product.service.ProductService;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
public class ProductController {

	private final ProductService productService;

	@GetMapping
	public PageResponse<ProductSummary> list(
		@RequestParam(required = false) UUID categoryId,
		@RequestParam(required = false) String search,
		@PageableDefault(size = 20) Pageable pageable) {
		return productService.list(categoryId, search, pageable);
	}

	@GetMapping("/{id}")
	public ProductSummary getById(@PathVariable UUID id) {
		return productService.getById(id);
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public ProductSummary create(@Valid @RequestBody ProductCreateRequest request) {
		return productService.create(request);
	}

	@PutMapping("/{id}")
	public ProductSummary update(@PathVariable UUID id, @Valid @RequestBody ProductUpdateRequest request) {
		return productService.update(id, request);
	}

	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void delete(@PathVariable UUID id) {
		productService.delete(id);
	}
}
