package com.eventhub.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;

class JwtServiceTests {

    private final JwtService jwtService = new JwtService(new AuthProperties(
            "test-secret-key-must-have-at-least-thirty-two-bytes",
            Duration.ofMinutes(15),
            Duration.ofDays(14),
            false,
            5,
            Duration.ofMinutes(10),
            Duration.ofMinutes(15)));

    @Test
    void createsAndParsesAccessToken() {
        AuthenticatedUser user = new AuthenticatedUser(
                42L,
                "tester",
                null,
                "测试用户",
                List.of("USER"),
                List.of("PROFILE_READ"),
                ClientType.USER_WEB,
                "session-1");

        AuthenticatedUser parsed = jwtService.parseAccessToken(jwtService.createAccessToken(user));

        assertThat(parsed.id()).isEqualTo(42L);
        assertThat(parsed.roles()).containsExactly("USER");
        assertThat(parsed.permissions()).containsExactly("PROFILE_READ");
        assertThat(parsed.clientType()).isEqualTo(ClientType.USER_WEB);
    }
}
