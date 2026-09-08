package com.nexcart.backend.common.storage;

import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

/**
 * Test-only stand-in for B2StorageServiceImpl — fabricates a deterministic fake URL and no-ops
 * on delete, so integration tests can exercise the upload/delete endpoints without real B2
 * credentials or network access. Mirrors DataSeeder's "!test" profile precedent in reverse.
 */
@Service
@Profile("test")
public class FakeStorageServiceImpl implements StorageService {

	@Override
	public UploadedFile upload(byte[] content, String contentType, String filename, String keyPrefix) {
		String key = keyPrefix + "/fake-" + UUID.randomUUID();
		return new UploadedFile("https://fake-storage.test/" + key, key);
	}

	@Override
	public void delete(String storageKey) {
		// no-op
	}
}
