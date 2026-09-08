package com.nexcart.backend.product.domain;

import com.nexcart.backend.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "products")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Product extends BaseEntity {

	@Column(nullable = false, length = 200)
	@Setter
	private String title;

	@Column(nullable = false, length = 2000)
	@Setter
	private String description;

	@Column(nullable = false, precision = 12, scale = 2)
	@Setter
	private BigDecimal price;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	@Setter
	private ProductStatus status;

	// Intentionally a real JPA association (unlike Inventory's plain FK column) since Category
	// lives in this same module/package with no independent lifecycle beyond being looked up.
	// LAZY is explicit: @ManyToOne defaults to EAGER, and open-in-view is disabled, so callers
	// must map this into a DTO while still inside a @Transactional method.
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "category_id", nullable = false, foreignKey = @ForeignKey(name = "fk_products_category"))
	@Setter
	private Category category;

	public static Product create(String title, String description, BigDecimal price, Category category) {
		Product product = new Product();
		product.title = title;
		product.description = description;
		product.price = price;
		product.category = category;
		product.status = ProductStatus.ACTIVE;
		return product;
	}
}
