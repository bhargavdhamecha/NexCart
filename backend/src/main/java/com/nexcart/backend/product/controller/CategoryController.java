package com.nexcart.backend.product.controller;

import com.nexcart.backend.product.dto.CategorySummary;
import com.nexcart.backend.product.service.CategoryService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/categories")
@RequiredArgsConstructor
public class CategoryController {

	private final CategoryService categoryService;

	@GetMapping
	public List<CategorySummary> list() {
		return categoryService.list();
	}
}
