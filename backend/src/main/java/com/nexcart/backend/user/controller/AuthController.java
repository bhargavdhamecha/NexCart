package com.nexcart.backend.user.controller;

import com.nexcart.backend.security.config.RefreshTokenCookieService;
import com.nexcart.backend.user.dto.LoginRequest;
import com.nexcart.backend.user.dto.RegisterRequest;
import com.nexcart.backend.user.dto.UserAuthResponse;
import com.nexcart.backend.user.service.AuthResult;
import com.nexcart.backend.user.service.AuthService;
import com.nexcart.backend.user.service.RequestMetadata;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

	private final AuthService authService;
	private final RefreshTokenCookieService refreshTokenCookieService;

	@PostMapping("/register")
	public ResponseEntity<UserAuthResponse> register(@Valid @RequestBody RegisterRequest request,
		HttpServletRequest httpServletRequest,
		HttpServletResponse httpServletResponse) {
		AuthResult result = authService.register(request, RequestMetadata.from(httpServletRequest));
		refreshTokenCookieService.addRefreshTokenCookie(httpServletResponse, result.refreshToken());
		return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(result));
	}

	@PostMapping("/login")
	public ResponseEntity<UserAuthResponse> login(@Valid @RequestBody LoginRequest request,
		HttpServletRequest httpServletRequest,
		HttpServletResponse httpServletResponse) {
		AuthResult result = authService.login(request, RequestMetadata.from(httpServletRequest));
		refreshTokenCookieService.addRefreshTokenCookie(httpServletResponse, result.refreshToken());
		return ResponseEntity.ok(toResponse(result));
	}

	@PostMapping("/refresh")
	public ResponseEntity<UserAuthResponse> refresh(HttpServletRequest httpServletRequest,
		HttpServletResponse httpServletResponse) {
		String refreshToken = refreshTokenCookieService.extractRefreshToken(httpServletRequest).orElse(null);
		AuthResult result = authService.refresh(refreshToken, RequestMetadata.from(httpServletRequest));
		refreshTokenCookieService.addRefreshTokenCookie(httpServletResponse, result.refreshToken());
		return ResponseEntity.ok(toResponse(result));
	}

	@PostMapping("/logout")
	public ResponseEntity<Void> logout(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) {
		String refreshToken = refreshTokenCookieService.extractRefreshToken(httpServletRequest).orElse(null);
		authService.logout(refreshToken);
		refreshTokenCookieService.clearRefreshTokenCookie(httpServletResponse);
		return ResponseEntity.noContent().build();
	}

	private UserAuthResponse toResponse(AuthResult result) {
		return UserAuthResponse.of(result.user(), result.accessToken(), result.expiresIn());
	}
}
