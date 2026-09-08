package com.nexcart.backend.user.service;

import com.nexcart.backend.user.dto.UserContact;
import com.nexcart.backend.user.repository.UserRepository;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserLookupServiceImpl implements UserLookupService {

	private final UserRepository userRepository;

	@Override
	@Transactional(readOnly = true)
	public Optional<UserContact> getContact(UUID userId) {
		return userRepository.findById(userId).map(UserContact::from);
	}
}
