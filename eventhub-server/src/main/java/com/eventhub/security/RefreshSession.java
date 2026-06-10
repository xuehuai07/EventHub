package com.eventhub.security;

public record RefreshSession(
        long userId, ClientType clientType, String tokenHash, String csrfHash, long expiresAtEpochSecond) {}
