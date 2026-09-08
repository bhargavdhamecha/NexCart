package com.nexcart.backend.order.domain;

import com.nexcart.backend.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "order_items")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrderItem extends BaseEntity {

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "order_id", nullable = false, foreignKey = @ForeignKey(name = "fk_order_items_order"))
	private Order order;

	@Column(name = "product_id", nullable = false)
	private UUID productId;

	// Snapshotted at order-creation time — same principle context.md gives for unitPrice ("do
	// not rely on current Product... for old orders"), applied to title/image too, so order
	// history stays correct even after a product is later renamed, re-imaged, or soft-deleted.
	@Column(nullable = false, length = 200)
	private String productTitle;

	@Column(length = 500)
	private String productImageUrl;

	@Column(nullable = false)
	private Integer quantity;

	@Column(nullable = false, precision = 12, scale = 2)
	private BigDecimal unitPrice;

	@Column(nullable = false, precision = 12, scale = 2)
	private BigDecimal itemTotal;

	public static OrderItem create(Order order, UUID productId, String productTitle, String productImageUrl,
		int quantity, BigDecimal unitPrice) {
		OrderItem item = new OrderItem();
		item.order = order;
		item.productId = productId;
		item.productTitle = productTitle;
		item.productImageUrl = productImageUrl;
		item.quantity = quantity;
		item.unitPrice = unitPrice;
		item.itemTotal = unitPrice.multiply(BigDecimal.valueOf(quantity));
		return item;
	}
}
