package com.nexcart.backend.user.service;

import com.nexcart.backend.common.exception.AuthException;
import com.nexcart.backend.security.config.SecurityProperties;
import com.nexcart.backend.user.domain.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Service;

@Service
public class JwtTokenService {

	private final SecurityProperties securityProperties;
	private final Clock clock;
	private final SecretKey signingKey;

	public JwtTokenService(SecurityProperties securityProperties, Clock clock) {
		this.securityProperties = securityProperties;
		this.clock = clock;
		this.signingKey = Keys.hmacShaKeyFor(securityProperties.getJwtSecret().getBytes(StandardCharsets.UTF_8));
	}

	public String generateAccessToken(User user) {
		Instant now = clock.instant();
		Instant expiresAt = now.plus(securityProperties.getAccessTokenTtl());

		return Jwts.builder()
			.subject(user.getId().toString())
			.claim("email", user.getEmail())
			.claim("role", user.getRole().name())
			.issuedAt(Date.from(now))
			.expiration(Date.from(expiresAt))
			.signWith(signingKey)
			.compact();
	}

	public UUID parseUserId(String token) {
		try {
			Claims claims = Jwts.parser()
				.verifyWith(signingKey)
				.build()
				.parseSignedClaims(token)
				.getPayload();
			return UUID.fromString(claims.getSubject());
		}
		catch (IllegalArgumentException | JwtException exception) {
			throw AuthException.invalidAccessToken();
		}
	}

	public long getAccessTokenTtlSeconds() {
		return securityProperties.getAccessTokenTtl().toSeconds();
	}
}
