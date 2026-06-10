package com.eventhub.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

    private final AuthProperties properties;
    private final SecretKey key;

    public JwtService(AuthProperties properties) {
        this.properties = properties;
        this.key = Keys.hmacShaKeyFor(properties.jwtSecret().getBytes(StandardCharsets.UTF_8));
    }

    public String createAccessToken(AuthenticatedUser user) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(Long.toString(user.id()))
                .claim("username", user.username())
                .claim("phone", user.phone())
                .claim("displayName", user.displayName())
                .claim("roles", user.roles())
                .claim("permissions", user.permissions())
                .claim("clientType", user.clientType().name())
                .claim("sessionId", user.sessionId())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(properties.accessTokenTtl())))
                .signWith(key)
                .compact();
    }

    public AuthenticatedUser parseAccessToken(String token) {
        Claims claims =
                Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
        return new AuthenticatedUser(
                Long.parseLong(claims.getSubject()),
                claims.get("username", String.class),
                claims.get("phone", String.class),
                claims.get("displayName", String.class),
                stringList(claims.get("roles")),
                stringList(claims.get("permissions")),
                ClientType.valueOf(claims.get("clientType", String.class)),
                claims.get("sessionId", String.class));
    }

    private List<String> stringList(Object value) {
        if (!(value instanceof List<?> values)) {
            return List.of();
        }
        return values.stream().map(String::valueOf).toList();
    }
}
