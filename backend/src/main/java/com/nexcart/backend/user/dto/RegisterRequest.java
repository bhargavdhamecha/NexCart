package com.nexcart.backend.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
	@NotBlank(message = "First name is required")
	@Size(max = 100, message = "First name must be at most 100 characters")
	String firstName,
	@NotBlank(message = "Last name is required")
	@Size(max = 100, message = "Last name must be at most 100 characters")
	String lastName,
	@NotBlank(message = "Email is required")
	@Email(message = "Email must be valid")
	@Size(max = 255, message = "Email must be at most 255 characters")
	String email,
	@NotBlank(message = "Password is required")
	@Pattern(
		regexp = "^(?=.*[A-Za-z])(?=.*\\d).{8,}$",
		message = "Password must be at least 8 characters and include at least one letter and one digit"
	)
	String password
) {
}
