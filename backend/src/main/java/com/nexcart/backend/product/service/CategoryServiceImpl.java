package com.nexcart.backend.product.service;

import com.nexcart.backend.common.exception.NotFoundException;
import com.nexcart.backend.product.domain.Category;
import com.nexcart.backend.product.dto.CategorySummary;
import com.nexcart.backend.product.repository.CategoryRepository;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

	private final CategoryRepository categoryRepository;

	@Override
	@Transactional(readOnly = true)
	public List<CategorySummary> list() {
		return categoryRepository.findAll().stream()
			.map(CategorySummary::from)
			.toList();
	}

	@Override
	@Transactional(readOnly = true)
	public Category getEntityById(UUID id) {
		return categoryRepository.findById(id).orElseThrow(() -> NotFoundException.category(id));
	}
}
