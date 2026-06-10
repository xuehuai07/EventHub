package com.eventhub.security;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.List;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

@Service
public class RefreshTokenStore {

    private static final String KEY_PREFIX = "eventhub:auth:refresh:";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public RefreshTokenStore(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    public void save(String tokenId, RefreshSession session, Duration ttl) {
        try {
            redisTemplate.opsForValue().set(KEY_PREFIX + tokenId, objectMapper.writeValueAsString(session), ttl);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("无法保存刷新会话", exception);
        }
    }

    public RefreshSession find(String tokenId) {
        String value = redisTemplate.opsForValue().get(KEY_PREFIX + tokenId);
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.readValue(value, RefreshSession.class);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("无法读取刷新会话", exception);
        }
    }

    public boolean consume(String tokenId, RefreshSession session) {
        try {
            String expected = objectMapper.writeValueAsString(session);
            DefaultRedisScript<Long> script = new DefaultRedisScript<>("""
                    if redis.call('GET', KEYS[1]) == ARGV[1] then
                        return redis.call('DEL', KEYS[1])
                    end
                    return 0
                    """, Long.class);
            Long deleted = redisTemplate.execute(script, List.of(KEY_PREFIX + tokenId), expected);
            return deleted != null && deleted == 1;
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("无法消费刷新会话", exception);
        }
    }

    public void delete(String tokenId) {
        redisTemplate.delete(KEY_PREFIX + tokenId);
    }
}
