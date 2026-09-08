package com.nexcart.backend.common.event;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

/**
 * Test-only stand-in for KafkaEventPublisherImpl — captures published events in memory instead
 * of touching a real broker, so integration tests can assert "checkout published X" without
 * needing Kafka running. Mirrors StorageService's FakeStorageServiceImpl pattern.
 */
@Service
@Profile("test")
public class FakeEventPublisherImpl implements EventPublisher {

	private final List<PublishedEvent> events = Collections.synchronizedList(new ArrayList<>());

	@Override
	public void publish(String topic, String key, Object payload) {
		events.add(new PublishedEvent(topic, key, payload));
	}

	public List<PublishedEvent> events() {
		return List.copyOf(events);
	}

	public void clear() {
		events.clear();
	}

	public record PublishedEvent(String topic, String key, Object payload) {
	}
}
