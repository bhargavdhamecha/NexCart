package com.nexcart.backend.user.dto;

import com.nexcart.backend.user.domain.User;

public record UserContact(String email, String firstName) {

	public static UserContact from(User user) {
		return new UserContact(user.getEmail(), user.getFirstName());
	}
}
