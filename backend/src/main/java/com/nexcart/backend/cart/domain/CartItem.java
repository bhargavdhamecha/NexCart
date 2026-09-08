package com.nexcart.backend.cart.domain;

import com.nexcart.backend.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// Intra-module real @ManyToOne (unlike the plain productId FK column below, which crosses into
// the product module) — same pattern as ProductImage -> Product.
@Entity
@Table(name = "cart_items", uniqueConstraints = {
	@UniqueConstraint(name = "uk_cart_items_cart_product", columnNames = {"cart_id", "product_id"})
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CartItem extends BaseEntity {

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "cart_id", nullable = false, foreignKey = @ForeignKey(name = "fk_cart_items_cart"))
	private Cart cart;

	@Column(name = "product_id", nullable = false)
	private UUID productId;

	@Column(nullable = false)
	@Setter
	private Integer quantity;

	public static CartItem create(Cart cart, UUID productId, int quantity) {
		CartItem item = new CartItem();
		item.cart = cart;
		item.productId = productId;
		item.quantity = quantity;
		return item;
	}
}
