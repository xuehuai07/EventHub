package com.eventhub.security;

public record AuthResponse(String accessToken, long expiresInSeconds, AuthUserView user) {}
