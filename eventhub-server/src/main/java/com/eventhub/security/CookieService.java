package com.eventhub.security;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Duration;
import java.util.Arrays;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;

@Service
public class CookieService {

    private final AuthProperties properties;

    public CookieService(AuthProperties properties) {
        this.properties = properties;
    }

    public void writeSessionCookies(
            HttpServletResponse response, ClientType clientType, String refreshToken, String csrfToken) {
        response.addHeader(
                HttpHeaders.SET_COOKIE,
                cookie(clientType.refreshCookie(), refreshToken, properties.refreshTokenTtl(), true)
                        .toString());
        response.addHeader(
                HttpHeaders.SET_COOKIE,
                cookie(clientType.csrfCookie(), csrfToken, properties.refreshTokenTtl(), false)
                        .toString());
    }

    public void clearSessionCookies(HttpServletResponse response, ClientType clientType) {
        response.addHeader(
                HttpHeaders.SET_COOKIE,
                cookie(clientType.refreshCookie(), "", Duration.ZERO, true).toString());
        response.addHeader(
                HttpHeaders.SET_COOKIE,
                cookie(clientType.csrfCookie(), "", Duration.ZERO, false).toString());
    }

    public String readCookie(HttpServletRequest request, String name) {
        if (request.getCookies() == null) {
            return null;
        }
        return Arrays.stream(request.getCookies())
                .filter(cookie -> cookie.getName().equals(name))
                .map(Cookie::getValue)
                .findFirst()
                .orElse(null);
    }

    private ResponseCookie cookie(String name, String value, Duration maxAge, boolean httpOnly) {
        return ResponseCookie.from(name, value)
                .httpOnly(httpOnly)
                .secure(properties.cookieSecure())
                .sameSite("Lax")
                .path(httpOnly ? "/api/auth" : "/")
                .maxAge(maxAge)
                .build();
    }
}
