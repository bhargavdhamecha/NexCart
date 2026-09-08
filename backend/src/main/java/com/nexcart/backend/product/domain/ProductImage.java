package com.nexcart.backend.product.domain;

import com.nexcart.backend.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// Intentionally a real @ManyToOne back-reference (unlike Inventory's plain FK column) since this
// is intra-module — same "product" package as Product. Unidirectional: Product has no @OneToMany
// back-reference, avoiding a second lazy-collection footgun alongside Product.category; images
// are fetched explicitly via ProductImageRepository when assembling ProductSummary.
@Entity
@Table(name = "product_images")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProductImage extends BaseEntity {

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "product_id", nullable = false, foreignKey = @ForeignKey(name = "fk_product_images_product"))
	private Product product;

	@Column(nullable = false, length = 500)
	private String url;

	// The B2 object key, needed to delete the object later — re-deriving it by parsing the URL
	// would be fragile.
	@Column(nullable = false, length = 500)
	private String storageKey;

	@Column(nullable = false)
	@Setter
	private Integer displayOrder;

	// Column renamed away from the bare word "primary" — it's a reserved SQL keyword in both
	// H2 and Postgres and breaks unquoted use as a column name.
	@Column(name = "is_primary", nullable = false)
	@Setter
	private boolean primary;

	public static ProductImage create(Product product, String url, String storageKey, int displayOrder,
		boolean primary) {
		ProductImage image = new ProductImage();
		image.product = product;
		image.url = url;
		image.storageKey = storageKey;
		image.displayOrder = displayOrder;
		image.primary = primary;
		return image;
	}
}
