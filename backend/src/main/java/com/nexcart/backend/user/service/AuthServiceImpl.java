package com.nexcart.backend.user.service;

import com.nexcart.backend.common.exception.AuthException;
import com.nexcart.backend.common.exception.ConflictException;
import com.nexcart.backend.common.exception.ErrorCode;
import com.nexcart.backend.common.logging.LogMask;
import com.nexcart.backend.security.jwt.AuthenticatedUser;
import com.nexcart.backend.user.domain.User;
import com.nexcart.backend.user.dto.LoginRequest;
import com.nexcart.backend.user.dto.RegisterRequest;
import com.nexcart.backend.user.dto.UserSummary;
import com.nexcart.backend.user.repository.UserRepository;
import java.util.Locale;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthServiceImpl implements AuthService {

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	private final JwtTokenService jwtTokenService;
	private final RefreshTokenService refreshTokenService;

	public AuthServiceImpl(UserRepository userRepository,
		PasswordEncoder passwordEncoder,
		JwtTokenService jwtTokenService,
		RefreshTokenService refreshTokenService) {
		this.userRepository = userRepository;
		this.passwordEncoder = passwordEncoder;
		this.jwtTokenService = jwtTokenService;
		this.refreshTokenService = refreshTokenService;
	}

	@Override
	@Transactional
	public AuthResult register(@LogMask RegisterRequest request, RequestMetadata metadata) {
		String normalizedEmail = normalizeEmail(request.email());
		if (userRepository.existsByEmail(normalizedEmail)) {
			throw emailAlreadyExists();
		}

		User user = User.create(
			request.firstName().trim(),
			request.lastName().trim(),
			normalizedEmail,
			passwordEncoder.encode(request.password())
		);

		try {
			userRepository.save(user);
		}
		catch (DataIntegrityViolationException exception) {
			throw emailAlreadyExists();
		}

		return createAuthResult(user, metadata);
	}

	@Override
	@Transactional
	public AuthResult login(@LogMask LoginRequest request, RequestMetadata metadata) {
		User user = userRepository.findByEmail(normalizeEmail(request.email()))
			.orElseThrow(AuthException::invalidCredentials);

		refreshTokenService.ensureActive(user);

		if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
			throw AuthException.invalidCredentials();
		}

		return createAuthResult(user, metadata);
	}

	@Override
	@Transactional
	public AuthResult refresh(@LogMask String refreshToken, RequestMetadata metadata) {
		RefreshTokenService.IssuedRefreshToken issuedRefreshToken = refreshTokenService.rotate(refreshToken, metadata);
		return toAuthResult(issuedRefreshToken.user(), issuedRefreshToken.rawToken());
	}

	@Override
	@Transactional
	public void logout(@LogMask String refreshToken) {
		refreshTokenService.revokeIfPresent(refreshToken);
	}

	@Override
	public UserSummary currentUser(AuthenticatedUser user) {
		return UserSummary.from(user);
	}

	private AuthResult createAuthResult(User user, RequestMetadata metadata) {
		RefreshTokenService.IssuedRefreshToken issuedRefreshToken = refreshTokenService.issue(user, metadata);
		return toAuthResult(user, issuedRefreshToken.rawToken());
	}

	private AuthResult toAuthResult(User user, String refreshToken) {
		UserSummary userSummary = UserSummary.from(user);
		String accessToken = jwtTokenService.generateAccessToken(user);
		return new AuthResult(userSummary, accessToken, jwtTokenService.getAccessTokenTtlSeconds(), refreshToken);
	}

	private ConflictException emailAlreadyExists() {
		return new ConflictException(ErrorCode.USER_EMAIL_ALREADY_EXISTS, "Email is already registered");
	}

	private String normalizeEmail(String email) {
		return email.trim().toLowerCase(Locale.ROOT);
	}
}
