package com.eventhub.security;

import java.util.List;

public record AuthenticatedUser(
        long id,
        String username,
        String phone,
        String displayName,
        List<String> roles,
        List<String> permissions,
        ClientType clientType,
        String sessionId) {}
