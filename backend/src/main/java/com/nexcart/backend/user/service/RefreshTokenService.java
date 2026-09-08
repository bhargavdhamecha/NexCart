package com.nexcart.backend.user.service;

import com.nexcart.backend.common.exception.AuthException;
import com.nexcart.backend.common.security.TokenGenerator;
import com.nexcart.backend.security.config.SecurityProperties;
import com.nexcart.backend.user.domain.RefreshSession;
import com.nexcart.backend.user.domain.User;
import com.nexcart.backend.user.repository.RefreshSessionRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class RefreshTokenService {

	private final RefreshSessionRepository refreshSessionRepository;
	private final SecurityProperties securityProperties;
	private final Clock clock;
	private final TokenGenerator tokenGenerator;

	public RefreshTokenService(RefreshSessionRepository refreshSessionRepository,
		SecurityProperties securityProperties,
		Clock clock,
		TokenGenerator tokenGenerator) {
		this.refreshSessionRepository = refreshSessionRepository;
		this.securityProperties = securityProperties;
		this.clock = clock;
		this.tokenGenerator = tokenGenerator;
	}

	public IssuedRefreshToken issue(User user, RequestMetadata metadata) {
		ensureActive(user);

		String rawToken = tokenGenerator.generateOpaqueToken();
		Instant expiresAt = clock.instant().plus(securityProperties.getRefreshTokenTtl());
		RefreshSession session = RefreshSession.create(
			user,
			tokenGenerator.hashToken(rawToken),
			expiresAt,
			metadata.userAgent(),
			metadata.ipAddress()
		);
		refreshSessionRepository.save(session);
		return new IssuedRefreshToken(rawToken, expiresAt, user);
	}

	public IssuedRefreshToken rotate(String rawToken, RequestMetadata metadata) {
		RefreshSession currentSession = getValidSession(rawToken);
		Instant now = clock.instant();
		currentSession.markUsed(now);
		currentSession.revoke(now);

		User user = currentSession.getUser();
		ensureActive(user);

		String replacementToken = tokenGenerator.generateOpaqueToken();
		Instant expiresAt = now.plus(securityProperties.getRefreshTokenTtl());
		RefreshSession replacementSession = RefreshSession.create(
			user,
			tokenGenerator.hashToken(replacementToken),
			expiresAt,
			metadata.userAgent(),
			metadata.ipAddress()
		);

		refreshSessionRepository.save(currentSession);
		refreshSessionRepository.save(replacementSession);

		return new IssuedRefreshToken(replacementToken, expiresAt, user);
	}

	public void revokeIfPresent(String rawToken) {
		if (!StringUtils.hasText(rawToken)) {
			return;
		}

		findByRawToken(rawToken).ifPresent(session -> {
			if (!session.isRevoked()) {
				session.revoke(clock.instant());
				refreshSessionRepository.save(session);
			}
		});
	}

	private RefreshSession getValidSession(String rawToken) {
		RefreshSession session = findByRawToken(rawToken).orElseThrow(AuthException::invalidSession);
		Instant now = clock.instant();

		if (session.isRevoked() || session.isExpired(now)) {
			throw AuthException.invalidSession();
		}

		return session;
	}

	private Optional<RefreshSession> findByRawToken(String rawToken) {
		if (!StringUtils.hasText(rawToken)) {
			return Optional.empty();
		}
		return refreshSessionRepository.findByTokenHash(tokenGenerator.hashToken(rawToken));
	}

	void ensureActive(User user) {
		if (!user.isActive()) {
			throw AuthException.accountDisabled();
		}
	}

	public record IssuedRefreshToken(
		String rawToken,
		Instant expiresAt,
		User user
	) {
	}
}
