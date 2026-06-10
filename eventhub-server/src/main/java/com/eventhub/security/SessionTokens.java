package com.eventhub.security;

public record SessionTokens(String accessToken, String refreshToken, String csrfToken, AuthenticatedUser user) {}
