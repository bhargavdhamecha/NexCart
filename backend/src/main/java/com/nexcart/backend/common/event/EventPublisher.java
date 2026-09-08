package com.nexcart.backend.common.event;

/**
 * Abstraction over the Kafka producer, so tests don't need a real broker (see
 * FakeEventPublisherImpl, active under the "test" profile — mirrors the StorageService/
 * FakeStorageServiceImpl pattern used for B2).
 */
public interface EventPublisher {

	void publish(String topic, String key, Object payload);
}
