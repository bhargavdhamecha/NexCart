package com.nexcart.backend.common.storage;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.storage")
public class StorageProperties {

	/** Backblaze B2 S3-compatible endpoint, e.g. https://s3.us-west-004.backblazeb2.com */
	private String endpoint;
	private String region;
	private String bucket;
	private String accessKeyId;
	private String secretAccessKey;
}
