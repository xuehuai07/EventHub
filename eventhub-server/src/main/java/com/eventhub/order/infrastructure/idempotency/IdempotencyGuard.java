package com.eventhub.order.infrastructure.idempotency;

import java.time.Duration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class IdempotencyGuard {

    private final StringRedisTemplate redis;

    public IdempotencyGuard(StringRedisTemplate redis) {
        this.redis = redis;
    }

    public boolean acquire(long userId, String scope, String key) {
        return Boolean.TRUE.equals(
                redis.opsForValue().setIfAbsent(redisKey(userId, scope, key), "1", Duration.ofSeconds(30)));
    }

    public void release(long userId, String scope, String key) {
        redis.delete(redisKey(userId, scope, key));
    }

    private String redisKey(long userId, String scope, String key) {
        return "eventhub:idempotent:" + userId + ":" + scope + ":" + key;
    }
}
