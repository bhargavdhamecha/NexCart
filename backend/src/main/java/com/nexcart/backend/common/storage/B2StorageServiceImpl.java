package com.nexcart.backend.common.storage;

import com.nexcart.backend.common.exception.ApiException;
import com.nexcart.backend.common.exception.ErrorCode;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

/**
 * Uploads to Backblaze B2 via its S3-compatible API using the standard AWS SDK v2 S3 client
 * (see S3ClientConfig) — the same code would work unchanged against real AWS S3 later, just
 * pointed at a different endpoint/credentials.
 */
@Service
@Profile("!test")
@RequiredArgsConstructor
@Slf4j
public class B2StorageServiceImpl implements StorageService {

	private final S3Client s3Client;
	private final StorageProperties storageProperties;

	@Override
	public UploadedFile upload(byte[] content, String contentType, String filename, String keyPrefix) {
		requireConfigured();
		String key = keyPrefix + "/" + UUID.randomUUID() + "-" + sanitize(filename);

		s3Client.putObject(
			PutObjectRequest.builder()
				.bucket(storageProperties.getBucket())
				.key(key)
				.contentType(contentType)
				.build(),
			RequestBody.fromBytes(content));

		String url = storageProperties.getEndpoint() + "/" + storageProperties.getBucket() + "/" + key;
		return new UploadedFile(url, key);
	}

	@Override
	public void delete(String storageKey) {
		// requireConfigured() is inside this try, not called before it: unlike upload(), a
		// missing config shouldn't block an admin from cleaning up a stale DB row — it's
		// best-effort, same as an actual B2 outage below.
		try {
			requireConfigured();
			s3Client.deleteObject(DeleteObjectRequest.builder()
				.bucket(storageProperties.getBucket())
				.key(storageKey)
				.build());
		}
		catch (Exception exception) {
			log.warn("Failed to delete object {} from B2", storageKey, exception);
		}
	}

	private void requireConfigured() {
		if (isBlank(storageProperties.getEndpoint()) || isBlank(storageProperties.getBucket())
			|| isBlank(storageProperties.getAccessKeyId()) || isBlank(storageProperties.getSecretAccessKey())) {
			throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, ErrorCode.IMAGE_STORAGE_NOT_CONFIGURED,
				"Image storage is not configured — set B2_ENDPOINT/B2_BUCKET/B2_ACCESS_KEY_ID/B2_SECRET_ACCESS_KEY");
		}
	}

	private String sanitize(String filename) {
		if (filename == null || filename.isBlank()) {
			return "file";
		}
		return filename.replaceAll("[^a-zA-Z0-9._-]", "_");
	}

	private boolean isBlank(String value) {
		return value == null || value.isBlank();
	}
}
