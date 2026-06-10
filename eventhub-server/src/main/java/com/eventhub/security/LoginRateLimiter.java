package com.eventhub.security;

import com.eventhub.common.error.BusinessException;
import com.eventhub.common.error.ErrorCode;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class LoginRateLimiter {

    private static final String FAILURE_PREFIX = "eventhub:auth:login-failure:";
    private static final String LOCK_PREFIX = "eventhub:auth:login-lock:";

    private final StringRedisTemplate redisTemplate;
    private final AuthProperties properties;

    public LoginRateLimiter(StringRedisTemplate redisTemplate, AuthProperties properties) {
        this.redisTemplate = redisTemplate;
        this.properties = properties;
    }

    public void check(String identifier, String ip) {
        if (Boolean.TRUE.equals(redisTemplate.hasKey(lockKey(identifier, ip)))) {
            throw new BusinessException(ErrorCode.AUTH_LOGIN_LOCKED);
        }
    }

    public void recordFailure(String identifier, String ip) {
        String key = failureKey(identifier, ip);
        Long failures = redisTemplate.opsForValue().increment(key);
        if (failures != null && failures == 1) {
            redisTemplate.expire(key, properties.loginFailureWindow());
        }
        if (failures != null && failures >= properties.loginMaxFailures()) {
            redisTemplate.opsForValue().set(lockKey(identifier, ip), "1", properties.loginLockDuration());
            redisTemplate.delete(key);
        }
    }

    public void clear(String identifier, String ip) {
        redisTemplate.delete(failureKey(identifier, ip));
        redisTemplate.delete(lockKey(identifier, ip));
    }

    private String failureKey(String identifier, String ip) {
        return FAILURE_PREFIX + TokenDigest.sha256(identifier.toLowerCase() + ":" + ip);
    }

    private String lockKey(String identifier, String ip) {
        return LOCK_PREFIX + TokenDigest.sha256(identifier.toLowerCase() + ":" + ip);
    }
}
