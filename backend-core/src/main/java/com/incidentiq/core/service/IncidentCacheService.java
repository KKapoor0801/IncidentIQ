package com.incidentiq.core.service;

import com.incidentiq.core.dto.response.IncidentResponse;
import com.incidentiq.core.dto.response.PageResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class IncidentCacheService {

    private static final Logger log = LoggerFactory.getLogger(IncidentCacheService.class);

    private static final String DETAIL_PREFIX = "incident:detail:";
    private static final String LIST_VERSION_KEY = "incident:list:version";
    private static final String LIST_PREFIX = "incident:list:v";
    private static final String DASHBOARD_OPEN_KEY = "dashboard:counts:open";

    private static final long DETAIL_TTL_SECONDS = 600;
    private static final long LIST_TTL_SECONDS = 30;
    private static final long DASHBOARD_TTL_SECONDS = 60;

    private final RedisTemplate<String, Object> redisTemplate;

    public IncidentCacheService(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @SuppressWarnings("unchecked")
    public IncidentResponse getCachedIncident(UUID incidentId) {
        try {
            Object cached = redisTemplate.opsForValue().get(DETAIL_PREFIX + incidentId);
            if (cached instanceof IncidentResponse response) {
                return response;
            }
        } catch (Exception e) {
            log.warn("Redis read failed for incident detail cache: {}", e.getMessage());
        }
        return null;
    }

    public void cacheIncident(IncidentResponse response) {
        try {
            redisTemplate.opsForValue().set(
                    DETAIL_PREFIX + response.id(), response, DETAIL_TTL_SECONDS, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.warn("Redis write failed for incident detail cache: {}", e.getMessage());
        }
    }

    public void evictIncident(UUID incidentId) {
        try {
            redisTemplate.delete(DETAIL_PREFIX + incidentId);
        } catch (Exception e) {
            log.warn("Redis evict failed for incident detail: {}", e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    public PageResponse<IncidentResponse> getCachedList(String status, String priority, String category, int page, int size) {
        try {
            Long version = getListVersion();
            String key = buildListKey(version, status, priority, category, page, size);
            Object cached = redisTemplate.opsForValue().get(key);
            if (cached instanceof PageResponse<?> pageResponse) {
                return (PageResponse<IncidentResponse>) pageResponse;
            }
        } catch (Exception e) {
            log.warn("Redis read failed for incident list cache: {}", e.getMessage());
        }
        return null;
    }

    public void cacheList(String status, String priority, String category, int page, int size,
                          PageResponse<IncidentResponse> response) {
        try {
            Long version = getListVersion();
            String key = buildListKey(version, status, priority, category, page, size);
            redisTemplate.opsForValue().set(key, response, LIST_TTL_SECONDS, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.warn("Redis write failed for incident list cache: {}", e.getMessage());
        }
    }

    public void bumpListVersion() {
        try {
            redisTemplate.opsForValue().increment(LIST_VERSION_KEY);
        } catch (Exception e) {
            log.warn("Redis failed to bump list version: {}", e.getMessage());
        }
    }

    public void cacheDashboardOpenCount(long count) {
        try {
            redisTemplate.opsForValue().set(DASHBOARD_OPEN_KEY, count, DASHBOARD_TTL_SECONDS, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.warn("Redis write failed for dashboard cache: {}", e.getMessage());
        }
    }

    public Long getCachedDashboardOpenCount() {
        try {
            Object cached = redisTemplate.opsForValue().get(DASHBOARD_OPEN_KEY);
            if (cached instanceof Number number) {
                return number.longValue();
            }
        } catch (Exception e) {
            log.warn("Redis read failed for dashboard cache: {}", e.getMessage());
        }
        return null;
    }

    public void onIncidentWrite(UUID incidentId) {
        evictIncident(incidentId);
        bumpListVersion();
    }

    private Long getListVersion() {
        try {
            Object val = redisTemplate.opsForValue().get(LIST_VERSION_KEY);
            if (val instanceof Number number) {
                return number.longValue();
            }
        } catch (Exception e) {
            // fall through
        }
        return 0L;
    }

    private String buildListKey(Long version, String status, String priority, String category, int page, int size) {
        String filters = "status=" + status + ":priority=" + priority + ":category=" + category;
        String hash = hashString(filters);
        return LIST_PREFIX + version + ":" + hash + ":" + page + ":" + size;
    }

    static String hashString(String input) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest).substring(0, 12);
        } catch (NoSuchAlgorithmException e) {
            return Integer.toHexString(input.hashCode());
        }
    }
}
