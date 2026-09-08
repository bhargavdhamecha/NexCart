package com.nexcart.backend.user.service;

import com.nexcart.backend.user.dto.UserContact;
import java.util.Optional;
import java.util.UUID;

/**
 * Minimal cross-module read used by the notification consumer to resolve a userId (from a Kafka
 * event, no HTTP request/JWT principal available) to an email/name — kept separate from
 * AuthService, which is specifically about the currently-authenticated request's principal.
 */
public interface UserLookupService {

	Optional<UserContact> getContact(UUID userId);
}
