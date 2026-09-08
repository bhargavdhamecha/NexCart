package com.nexcart.backend.user.service;

import com.nexcart.backend.security.jwt.AuthenticatedUser;
import com.nexcart.backend.user.dto.LoginRequest;
import com.nexcart.backend.user.dto.RegisterRequest;
import com.nexcart.backend.user.dto.UserSummary;

public interface AuthService {

	AuthResult register(RegisterRequest request, RequestMetadata metadata);

	AuthResult login(LoginRequest request, RequestMetadata metadata);

	AuthResult refresh(String refreshToken, RequestMetadata metadata);

	void logout(String refreshToken);

	UserSummary currentUser(AuthenticatedUser user);
}
