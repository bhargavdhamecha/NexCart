package com.nexcart.backend.user.domain;

import com.nexcart.backend.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "users", uniqueConstraints = {
	@UniqueConstraint(name = "uk_users_email", columnNames = "email")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseEntity {

	@Column(nullable = false, length = 100)
	private String firstName;

	@Column(nullable = false, length = 100)
	private String lastName;

	@Column(nullable = false, length = 255)
	private String email;

	@Column(nullable = false, length = 255)
	private String passwordHash;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	@Setter
	private UserRole role;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	@Setter
	private UserStatus status;

	public boolean isActive() {
		return status == UserStatus.ACTIVE;
	}

	public static User create(String firstName, String lastName, String email, String passwordHash) {
		User user = new User();
		user.firstName = firstName;
		user.lastName = lastName;
		user.email = email;
		user.passwordHash = passwordHash;
		user.role = UserRole.CUSTOMER;
		user.status = UserStatus.ACTIVE;
		return user;
	}
}
