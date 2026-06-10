package com.eventhub.security;

import java.util.List;

public record AuthUserView(
        long id, String username, String phone, String displayName, List<String> roles, List<String> permissions) {

    public static AuthUserView from(AuthenticatedUser user) {
        return new AuthUserView(
                user.id(), user.username(), user.phone(), user.displayName(), user.roles(), user.permissions());
    }
}
