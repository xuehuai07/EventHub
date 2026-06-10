package com.eventhub.activity.infrastructure.cache;

import com.eventhub.activity.dto.ActivityDetailView;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class ActivityDetailCache {

    private static final Duration TTL = Duration.ofMinutes(10);
    private static final String PREFIX = "eventhub:activity:detail:";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public ActivityDetailCache(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    public ActivityDetailView get(long activityId) {
        String value = redisTemplate.opsForValue().get(key(activityId));
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.readValue(value, ActivityDetailView.class);
        } catch (JsonProcessingException exception) {
            evict(activityId);
            return null;
        }
    }

    public void put(long activityId, ActivityDetailView detail) {
        try {
            redisTemplate.opsForValue().set(key(activityId), objectMapper.writeValueAsString(detail), TTL);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("活动详情缓存序列化失败", exception);
        }
    }

    public void evict(long activityId) {
        redisTemplate.delete(key(activityId));
    }

    private String key(long activityId) {
        return PREFIX + activityId;
    }
}
