package com.nexcart.backend.common.storage;

/**
 * Abstraction over the product-image object store, so tests don't need real cloud credentials
 * or network access (see FakeStorageServiceImpl, active under the "test" profile).
 *
 * Takes already-processed bytes (see ImageCompressor) rather than a raw MultipartFile — storage
 * shouldn't know about image compression, and image compression shouldn't know about B2/S3.
 */
public interface StorageService {

	UploadedFile upload(byte[] content, String contentType, String filename, String keyPrefix);

	/** Best-effort: implementations should log and swallow failures rather than throw. */
	void delete(String storageKey);

	record UploadedFile(String url, String storageKey) {
	}
}
