package com.eventhub.assistant;

import com.eventhub.common.error.BusinessException;
import com.eventhub.common.error.ErrorCode;
import java.time.Duration;
import java.util.Collections;
import java.util.UUID;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

@Component
public class AssistantStreamLock {

    private static final DefaultRedisScript<Long> RELEASE_SCRIPT = new DefaultRedisScript<>(
            "if redis.call('get', KEYS[1]) == ARGV[1] then " + "return redis.call('del', KEYS[1]) else return 0 end",
            Long.class);

    private final StringRedisTemplate redis;

    public AssistantStreamLock(StringRedisTemplate redis) {
        this.redis = redis;
    }

    public LockHandle acquire(long userId) {
        String key = "eventhub:assistant:stream:" + userId;
        String token = UUID.randomUUID().toString();
        try {
            Boolean acquired = redis.opsForValue().setIfAbsent(key, token, Duration.ofSeconds(75));
            return Boolean.TRUE.equals(acquired) ? new LockHandle(key, token) : null;
        } catch (DataAccessException exception) {
            throw new BusinessException(ErrorCode.AI_UPSTREAM_UNAVAILABLE, "AI 并发控制服务暂时不可用");
        }
    }

    public void release(LockHandle handle) {
        if (handle != null) {
            try {
                redis.execute(RELEASE_SCRIPT, Collections.singletonList(handle.key()), handle.token());
            } catch (DataAccessException ignored) {
                // The short TTL remains the final cleanup path when Redis is briefly unavailable.
            }
        }
    }

    public record LockHandle(String key, String token) {}
}
