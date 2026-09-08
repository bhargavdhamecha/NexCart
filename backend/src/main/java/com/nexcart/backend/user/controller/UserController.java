package com.nexcart.backend.user.controller;

import com.nexcart.backend.security.jwt.AuthenticatedUser;
import com.nexcart.backend.user.dto.UserSummary;
import com.nexcart.backend.user.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

	private final AuthService authService;

	@GetMapping("/me")
	public UserSummary currentUser(@AuthenticationPrincipal AuthenticatedUser user) {
		return authService.currentUser(user);
	}
}
