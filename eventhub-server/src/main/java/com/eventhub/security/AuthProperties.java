package com.eventhub.security;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("eventhub.auth")
public record AuthProperties(
        String jwtSecret,
        Duration accessTokenTtl,
        Duration refreshTokenTtl,
        boolean cookieSecure,
        int loginMaxFailures,
        Duration loginFailureWindow,
        Duration loginLockDuration) {}
