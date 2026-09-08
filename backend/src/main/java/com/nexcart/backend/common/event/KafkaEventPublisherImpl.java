package com.nexcart.backend.common.event;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@Profile("!test")
@RequiredArgsConstructor
public class KafkaEventPublisherImpl implements EventPublisher {

	// Spring Boot's KafkaAutoConfiguration provides a KafkaTemplate<Object, Object> bean by
	// default — declaring this as <String, Object> here would fail generics-aware autowiring
	// (invariant generics: a <Object,Object> bean isn't a match for a <String,Object> injection
	// point), so this matches what's actually autoconfigured rather than defining a custom bean.
	private final KafkaTemplate<Object, Object> kafkaTemplate;

	@Override
	public void publish(String topic, String key, Object payload) {
		kafkaTemplate.send(topic, key, payload);
	}
}
