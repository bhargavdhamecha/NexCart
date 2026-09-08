package com.nexcart.backend.security.jwt;

import com.nexcart.backend.user.domain.UserRole;
import java.util.UUID;

public record AuthenticatedUser(
	UUID id,
	String email,
	String firstName,
	String lastName,
	UserRole role
) {
}
