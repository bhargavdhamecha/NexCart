package com.nexcart.backend.config;

import java.util.HashMap;
import java.util.Map;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;

/**
 * Neither spring-boot-autoconfigure nor spring-kafka itself ships any Spring Boot
 * auto-configuration for Kafka in this version combination (Spring Boot 4.1.1 / spring-kafka
 * 4.1.1) — confirmed by inspecting both jars for autoconfiguration classes/imports files, and by
 * a KafkaTemplate injection failure at boot before this class existed. So producer/consumer
 * factories and the listener container factory are wired up by hand here, reading the same
 * spring.kafka.* properties a "normal" Spring Boot app would.
 */
@Configuration
@EnableKafka
public class KafkaConfig {

	@Value("${spring.kafka.bootstrap-servers}")
	private String bootstrapServers;

	@Value("${spring.kafka.consumer.group-id}")
	private String consumerGroupId;

	@Value("${spring.kafka.consumer.properties.spring.json.trusted.packages}")
	private String trustedPackages;

	// Not read by any autoconfiguration either — application-test.properties sets this to
	// false so the listener container doesn't try to connect to a broker during tests
	// (FakeEventPublisherImpl handles the producer side there; nothing needs to consume).
	@Value("${spring.kafka.listener.auto-startup:true}")
	private boolean listenerAutoStartup;

	@Bean
	ProducerFactory<Object, Object> producerFactory() {
		Map<String, Object> config = new HashMap<>();
		config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
		config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
		config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
		return new DefaultKafkaProducerFactory<>(config);
	}

	@Bean
	KafkaTemplate<Object, Object> kafkaTemplate(ProducerFactory<Object, Object> producerFactory) {
		return new KafkaTemplate<>(producerFactory);
	}

	@Bean
	ConsumerFactory<Object, Object> consumerFactory() {
		Map<String, Object> config = new HashMap<>();
		config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
		config.put(ConsumerConfig.GROUP_ID_CONFIG, consumerGroupId);
		config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
		config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
		config.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
		config.put(JsonDeserializer.TRUSTED_PACKAGES, trustedPackages);
		return new DefaultKafkaConsumerFactory<>(config);
	}

	@Bean
	ConcurrentKafkaListenerContainerFactory<Object, Object> kafkaListenerContainerFactory(
		ConsumerFactory<Object, Object> consumerFactory) {
		ConcurrentKafkaListenerContainerFactory<Object, Object> factory = new ConcurrentKafkaListenerContainerFactory<>();
		factory.setConsumerFactory(consumerFactory);
		factory.setAutoStartup(listenerAutoStartup);
		return factory;
	}
}
