package com.nexcart.backend.user.domain;

import com.nexcart.backend.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "refresh_sessions", indexes = {
	@Index(name = "idx_refresh_sessions_user_id", columnList = "user_id"),
	@Index(name = "idx_refresh_sessions_token_hash", columnList = "token_hash", unique = true)
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RefreshSession extends BaseEntity {

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@Column(name = "token_hash", nullable = false, length = 64, unique = true)
	private String tokenHash;

	@Column(nullable = false)
	private Instant expiresAt;

	@Column
	private Instant revokedAt;

	@Column
	private Instant lastUsedAt;

	@Column(length = 500)
	private String userAgent;

	@Column(length = 100)
	private String ipAddress;

	public static RefreshSession create(User user, String tokenHash, Instant expiresAt, String userAgent,
		String ipAddress) {
		RefreshSession session = new RefreshSession();
		session.user = user;
		session.tokenHash = tokenHash;
		session.expiresAt = expiresAt;
		session.userAgent = userAgent;
		session.ipAddress = ipAddress;
		return session;
	}

	public boolean isRevoked() {
		return revokedAt != null;
	}

	public boolean isExpired(Instant now) {
		return expiresAt.isBefore(now);
	}

	public void markUsed(Instant usedAt) {
		this.lastUsedAt = usedAt;
	}

	public void revoke(Instant revokedAt) {
		this.revokedAt = revokedAt;
	}
}
