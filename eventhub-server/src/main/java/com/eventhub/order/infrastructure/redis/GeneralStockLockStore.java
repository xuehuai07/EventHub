package com.eventhub.order.infrastructure.redis;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

@Component
public class GeneralStockLockStore {

    private static final DefaultRedisScript<Long> LOCK_SCRIPT = script("redis/lock-general-stock.lua");
    private static final DefaultRedisScript<Long> RELEASE_SCRIPT = script("redis/release-general-stock.lua");

    private final StringRedisTemplate redis;

    public GeneralStockLockStore(StringRedisTemplate redis) {
        this.redis = redis;
    }

    public boolean lock(long ticketTypeId, int availableStock, int quantity, String lockNo, Duration ttl) {
        long now = Instant.now().toEpochMilli();
        Long result = redis.execute(
                LOCK_SCRIPT,
                List.of(key(ticketTypeId)),
                Long.toString(now),
                Long.toString(now + ttl.toMillis()),
                Integer.toString(availableStock),
                Integer.toString(quantity),
                lockNo);
        return Long.valueOf(1).equals(result);
    }

    public void release(long ticketTypeId, String lockNo) {
        redis.execute(RELEASE_SCRIPT, List.of(key(ticketTypeId)), lockNo);
    }

    public int activeLocks(long ticketTypeId) {
        String key = key(ticketTypeId);
        redis.opsForZSet().removeRangeByScore(key, 0, Instant.now().toEpochMilli());
        Long count = redis.opsForZSet().zCard(key);
        return count == null ? 0 : Math.toIntExact(count);
    }

    private String key(long ticketTypeId) {
        return "eventhub:stock:locks:" + ticketTypeId;
    }

    private static DefaultRedisScript<Long> script(String path) {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setLocation(new ClassPathResource(path));
        script.setResultType(Long.class);
        return script;
    }
}
