package com.nexcart.backend.config;

import com.nexcart.backend.common.storage.StorageProperties;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;

/**
 * Builds the S3Client used to talk to Backblaze B2's S3-compatible API. Always constructs
 * successfully even without real credentials configured — B2StorageServiceImpl fails fast with
 * a clear error only when an upload/delete is actually attempted, so the rest of the app keeps
 * working in local dev without B2 set up.
 */
@Configuration
@EnableConfigurationProperties(StorageProperties.class)
@RequiredArgsConstructor
public class S3ClientConfig {

	private static final String DEFAULT_ENDPOINT = "https://s3.us-west-004.backblazeb2.com";
	private static final String DEFAULT_REGION = "us-west-004";
	// AwsBasicCredentials.create() rejects blank strings, so a non-blank placeholder is used when
	// unconfigured — B2StorageServiceImpl.requireConfigured() is what actually fails clearly, the
	// first time an upload/delete is attempted, not this bean's construction.
	private static final String UNCONFIGURED_PLACEHOLDER = "unconfigured";

	private final StorageProperties storageProperties;

	@Bean
	S3Client s3Client() {
		String endpoint = orDefault(storageProperties.getEndpoint(), DEFAULT_ENDPOINT);
		String region = orDefault(storageProperties.getRegion(), DEFAULT_REGION);

		return S3Client.builder()
			.endpointOverride(URI.create(endpoint))
			.region(Region.of(region))
			.credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(
				orDefault(storageProperties.getAccessKeyId(), UNCONFIGURED_PLACEHOLDER),
				orDefault(storageProperties.getSecretAccessKey(), UNCONFIGURED_PLACEHOLDER))))
			// B2's S3-compatible endpoint requires path-style addressing (bucket in the path,
			// not the hostname).
			.serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build())
			.build();
	}

	private String orDefault(String value, String fallback) {
		return (value == null || value.isBlank()) ? fallback : value;
	}
}
