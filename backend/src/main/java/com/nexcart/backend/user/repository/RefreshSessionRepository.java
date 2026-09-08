package com.nexcart.backend.user.repository;

import com.nexcart.backend.user.domain.RefreshSession;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RefreshSessionRepository extends JpaRepository<RefreshSession, UUID> {

	Optional<RefreshSession> findByTokenHash(String tokenHash);
}
