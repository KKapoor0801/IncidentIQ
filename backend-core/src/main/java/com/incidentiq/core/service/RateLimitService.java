package com.incidentiq.core.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class RateLimitService {

    private static final Logger log = LoggerFactory.getLogger(RateLimitService.class);
    private static final String PREFIX = "ratelimit:";
    private static final long WINDOW_SECONDS = 60;
    private static final long MAX_REQUESTS = 10;

    private final RedisTemplate<String, Object> redisTemplate;

    public RateLimitService(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public boolean isRateLimited(UUID userId, String endpoint) {
        String key = PREFIX + userId + ":" + endpoint;
        try {
            Long count = redisTemplate.opsForValue().increment(key);
            if (count != null && count == 1L) {
                redisTemplate.expire(key, WINDOW_SECONDS, TimeUnit.SECONDS);
            }
            log.debug("Rate limit check: userId={}, endpoint={}, count={}", userId, endpoint, count);
            if (count != null && count > MAX_REQUESTS) {
                log.warn("Rate limit exceeded: userId={}, endpoint={}, count={}", userId, endpoint, count);
                return true;
            }
            return false;
        } catch (Exception e) {
            log.warn("Rate limit check failed, allowing request: {}", e.getMessage());
            return false;
        }
    }
}
