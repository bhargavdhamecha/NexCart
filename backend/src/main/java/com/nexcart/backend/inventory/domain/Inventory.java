package com.nexcart.backend.inventory.domain;

import com.nexcart.backend.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "inventory", uniqueConstraints = {
	@UniqueConstraint(name = "uk_inventory_product_id", columnNames = "product_id")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Inventory extends BaseEntity {

	@Column(name = "product_id", nullable = false)
	private UUID productId;

	@Column(nullable = false)
	@Setter
	private Integer availableQuantity;

	@Column(nullable = false)
	@Setter
	private Integer reservedQuantity;

	public static Inventory initialize(UUID productId, int availableQuantity) {
		Inventory inventory = new Inventory();
		inventory.productId = productId;
		inventory.availableQuantity = availableQuantity;
		inventory.reservedQuantity = 0;
		return inventory;
	}
}
