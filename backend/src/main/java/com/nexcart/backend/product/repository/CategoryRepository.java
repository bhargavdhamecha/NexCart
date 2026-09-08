package com.nexcart.backend.product.repository;

import com.nexcart.backend.product.domain.Category;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<Category, UUID> {
}
