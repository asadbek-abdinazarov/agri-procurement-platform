package com.agriprocurement.procurement.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProcurementCacheService {

    private static final String CACHE_KEY_PREFIX = "procurement:";
    private static final Duration CACHE_TTL = Duration.ofMinutes(30);

    private final RedisTemplate<String, Object> redisTemplate;

    @Cacheable(value = "procurements", key = "#procurementId")
    public ProcurementResponse get(String procurementId) {
        String key = CACHE_KEY_PREFIX + procurementId;
        return (ProcurementResponse) redisTemplate.opsForValue().get(key);
    }

    public void put(String procurementId, ProcurementResponse response) {
        String key = CACHE_KEY_PREFIX + procurementId;
        redisTemplate.opsForValue().set(key, response, CACHE_TTL);
        log.debug("Cached procurement: {}", procurementId);
    }

    @CacheEvict(value = "procurements", key = "#procurementId")
    public void evict(String procurementId) {
        String key = CACHE_KEY_PREFIX + procurementId;
        redisTemplate.delete(key);
        log.debug("Evicted procurement from cache: {}", procurementId);
    }

    @CacheEvict(value = "procurements", allEntries = true)
    public void evictAll() {
        log.debug("Evicted all procurements from cache");
    }
}
