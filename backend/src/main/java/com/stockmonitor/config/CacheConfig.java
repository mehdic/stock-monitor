package com.stockmonitor.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.cache.support.CompositeCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

/**
 * Cache configuration with Redis primary and Caffeine (local in-memory) fallback.
 *
 * Cache Strategy:
 * - Redis: Distributed cache for multi-instance deployments
 * - Caffeine: Local in-memory fallback if Redis unavailable
 * - CompositeCacheManager chains Redis → Caffeine
 *
 * Cache Names (TTL):
 * - factorScores (1 hour): Factor calculation results
 * - universeConstituents (6 hours): Universe membership data
 * - marketData (15 minutes): Real-time price/market data
 * - userSettings (30 minutes): User preferences and constraints
 */
@Configuration
@EnableCaching
@Slf4j
public class CacheConfig {

  /**
   * Composite cache manager with Redis primary and Caffeine fallback.
   * If Redis is unavailable, Caffeine provides local caching.
   */
  @Bean
  @Primary
  public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
    try {
      // Test Redis connection
      connectionFactory.getConnection().ping();
      log.info("Redis connection successful - using Redis primary cache with Caffeine fallback");

      CacheManager redisCacheManager = redisCacheManager(connectionFactory);
      CacheManager caffeineCacheManager = caffeineCacheManager();

      CompositeCacheManager compositeCacheManager = new CompositeCacheManager(
              redisCacheManager,
              caffeineCacheManager
      );
      compositeCacheManager.setFallbackToNoOpCache(false);
      return compositeCacheManager;

    } catch (Exception e) {
      log.warn("Redis unavailable - falling back to Caffeine local cache only: {}", e.getMessage());
      return caffeineCacheManager();
    }
  }

  /**
   * Redis cache manager with custom TTL per cache name.
   */
  @Bean
  public CacheManager redisCacheManager(RedisConnectionFactory connectionFactory) {
    ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.registerModule(new JavaTimeModule());
    objectMapper.activateDefaultTyping(
        BasicPolymorphicTypeValidator.builder().allowIfBaseType(Object.class).build(),
        ObjectMapper.DefaultTyping.NON_FINAL,
        JsonTypeInfo.As.PROPERTY);

    GenericJackson2JsonRedisSerializer serializer =
        new GenericJackson2JsonRedisSerializer(objectMapper);

    RedisCacheConfiguration defaultConfig =
        RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofHours(1))
            .serializeKeysWith(
                RedisSerializationContext.SerializationPair.fromSerializer(
                    new StringRedisSerializer()))
            .serializeValuesWith(
                RedisSerializationContext.SerializationPair.fromSerializer(serializer))
            .disableCachingNullValues();

    return RedisCacheManager.builder(connectionFactory)
        .cacheDefaults(defaultConfig)
        .withCacheConfiguration("factorScores",
            defaultConfig.entryTtl(Duration.ofHours(1)))
        .withCacheConfiguration("universeConstituents",
            defaultConfig.entryTtl(Duration.ofHours(6)))
        .withCacheConfiguration("marketData",
            defaultConfig.entryTtl(Duration.ofMinutes(15)))
        .withCacheConfiguration("userSettings",
            defaultConfig.entryTtl(Duration.ofMinutes(30)))
        .withCacheConfiguration("recommendations",
            defaultConfig.entryTtl(Duration.ofHours(24)))
        .build();
  }

  /**
   * Caffeine (local in-memory) cache manager as fallback.
   * Smaller capacity than Redis to prevent memory issues.
   */
  @Bean
  public CacheManager caffeineCacheManager() {
    CaffeineCacheManager cacheManager = new CaffeineCacheManager();
    cacheManager.setCacheNames(Arrays.asList(
        "factorScores",
        "universeConstituents",
        "marketData",
        "userSettings",
        "recommendations"
    ));
    cacheManager.setCaffeine(caffeineCacheBuilder());
    return cacheManager;
  }

  /**
   * Caffeine cache builder with size limits and expiration.
   */
  private Caffeine<Object, Object> caffeineCacheBuilder() {
    return Caffeine.newBuilder()
        .maximumSize(1000) // Limit to 1000 entries per cache
        .expireAfterWrite(1, TimeUnit.HOURS) // Default 1 hour TTL
        .recordStats() // Enable statistics for monitoring
        .weakKeys();
  }
}
