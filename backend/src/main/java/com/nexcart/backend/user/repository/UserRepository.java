package com.nexcart.backend.user.repository;

import com.nexcart.backend.user.domain.User;
import com.nexcart.backend.user.domain.UserStatus;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, UUID> {

	boolean existsByEmail(String email);

	Optional<User> findByEmail(String email);

	Optional<User> findByIdAndStatus(UUID id, UserStatus status);
}
