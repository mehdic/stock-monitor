package com.stockmonitor.service;

import java.time.Duration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

/** Redis cache service for data feeds (T219, T220). */
@Service
@RequiredArgsConstructor
@Slf4j
@ConditionalOnBean(RedisTemplate.class)
public class DataFeedCacheService {

  private final RedisTemplate<String, Object> redisTemplate;

  public void cache(String key, Object value, Duration ttl) {
    redisTemplate.opsForValue().set(key, value, ttl);
    log.debug("Cached: {}", key);
  }

  public Object get(String key) {
    return redisTemplate.opsForValue().get(key);
  }

  public void invalidate(String pattern) {
    redisTemplate.delete(redisTemplate.keys(pattern));
    log.info("Invalidated cache: {}", pattern);
  }
}
