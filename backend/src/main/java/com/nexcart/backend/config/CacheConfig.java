package com.nexcart.backend.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.Duration;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Backs the product-catalog cache (see ProductCatalogCache) — real Redis in normal use,
 * a trivial in-memory CacheManager under the "test" profile so the suite exercises real
 * caching/eviction behavior without needing a Redis broker (same real/fake-by-profile
 * convention used for Kafka/S3/Razorpay elsewhere in this codebase).
 */
@Configuration
@EnableCaching
public class CacheConfig {

	// Freshness on writes comes from explicit @CacheEvict calls, not this TTL — it's only a
	// backstop against a missed/buggy eviction path, not the primary invalidation mechanism.
	private static final Duration DEFAULT_TTL = Duration.ofMinutes(10);

	@Bean
	@Profile("!test")
	CacheManager redisCacheManager(RedisConnectionFactory connectionFactory) {
		ObjectMapper objectMapper = new ObjectMapper();
		objectMapper.registerModule(new JavaTimeModule());
		// GenericJackson2JsonRedisSerializer is deprecated-for-removal in this Spring Data Redis
		// version in favor of GenericJacksonJsonRedisSerializer — which is built on Jackson 3
		// (tools.jackson.*), a different artifact from the Jackson 2 (com.fasterxml.jackson.*)
		// this app uses everywhere else. Staying on the Jackson-2-based serializer here
		// deliberately, rather than mixing Jackson major versions for one config class.
		//
		// .defaultTyping(true) is required when supplying a custom ObjectMapper — without it,
		// the serializer never embeds a type hint in the JSON, so on read-back Jackson can't
		// tell what concrete type to reconstruct (PageResponse<ProductCatalogEntry> is generic)
		// and silently deserializes into a plain LinkedHashMap instead, which then blows up
		// with a ClassCastException the moment the cache abstraction tries to use it as the
		// real return type. Using the builder (not hand-rolled activateDefaultTyping(...)) so
		// Spring's own vetted PolymorphicTypeValidator/JsonTypeInfo settings apply, rather than
		// risking a deserialization-gadget-prone configuration from getting those params wrong.
		GenericJackson2JsonRedisSerializer valueSerializer = GenericJackson2JsonRedisSerializer.builder()
			.objectMapper(objectMapper)
			.defaultTyping(true)
			.build();

		RedisCacheConfiguration configuration = RedisCacheConfiguration.defaultCacheConfig()
			.entryTtl(DEFAULT_TTL)
			.serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
			.serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(valueSerializer));

		return RedisCacheManager.builder(connectionFactory)
			.cacheDefaults(configuration)
			.build();
	}

	@Bean
	@Profile("test")
	CacheManager testCacheManager() {
		return new ConcurrentMapCacheManager("productList", "productDetail");
	}
}
