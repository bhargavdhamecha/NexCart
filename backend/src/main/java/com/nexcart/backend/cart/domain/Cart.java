package com.nexcart.backend.cart.domain;

import com.nexcart.backend.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "carts", uniqueConstraints = {
	@UniqueConstraint(name = "uk_carts_user_id", columnNames = "user_id")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Cart extends BaseEntity {

	@Column(name = "user_id", nullable = false)
	private UUID userId;

	public static Cart create(UUID userId) {
		Cart cart = new Cart();
		cart.userId = userId;
		return cart;
	}
}
