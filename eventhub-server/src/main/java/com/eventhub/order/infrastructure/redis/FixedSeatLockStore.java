package com.eventhub.order.infrastructure.redis;

import java.time.Duration;
import java.util.List;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

@Component
public class FixedSeatLockStore {

    private static final DefaultRedisScript<Long> LOCK_SCRIPT = script("redis/lock-fixed-seats.lua");
    private static final DefaultRedisScript<Long> RELEASE_SCRIPT = script("redis/release-fixed-seats.lua");

    private final StringRedisTemplate redis;

    public FixedSeatLockStore(StringRedisTemplate redis) {
        this.redis = redis;
    }

    public boolean lock(long sessionId, List<Long> seatIds, String lockNo, Duration ttl) {
        Long result = redis.execute(LOCK_SCRIPT, keys(sessionId, seatIds), lockNo, Long.toString(ttl.toMillis()));
        return Long.valueOf(1).equals(result);
    }

    public void release(long sessionId, List<Long> seatIds, String lockNo) {
        if (!seatIds.isEmpty()) {
            redis.execute(RELEASE_SCRIPT, keys(sessionId, seatIds), lockNo);
        }
    }

    public boolean isLocked(long sessionId, long seatId) {
        return Boolean.TRUE.equals(redis.hasKey(key(sessionId, seatId)));
    }

    private List<String> keys(long sessionId, List<Long> seatIds) {
        return seatIds.stream().map(seatId -> key(sessionId, seatId)).toList();
    }

    private String key(long sessionId, long seatId) {
        return "eventhub:seat:lock:" + sessionId + ":" + seatId;
    }

    private static DefaultRedisScript<Long> script(String path) {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setLocation(new ClassPathResource(path));
        script.setResultType(Long.class);
        return script;
    }
}
