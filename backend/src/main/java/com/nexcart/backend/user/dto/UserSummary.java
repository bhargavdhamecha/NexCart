package com.nexcart.backend.user.dto;

import com.nexcart.backend.security.jwt.AuthenticatedUser;
import com.nexcart.backend.user.domain.User;
import com.nexcart.backend.user.domain.UserRole;
import java.util.UUID;

public record UserSummary(
	UUID id,
	String firstName,
	String lastName,
	String email,
	UserRole role
) {

	public static UserSummary from(User user) {
		return new UserSummary(user.getId(), user.getFirstName(), user.getLastName(), user.getEmail(), user.getRole());
	}

	public static UserSummary from(AuthenticatedUser user) {
		return new UserSummary(user.id(), user.firstName(), user.lastName(), user.email(), user.role());
	}
}
