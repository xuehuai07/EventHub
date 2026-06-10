package com.eventhub.security;

public enum ClientType {
    USER_WEB("eventhub_user_refresh", "eventhub_user_csrf"),
    ADMIN_WEB("eventhub_admin_refresh", "eventhub_admin_csrf");

    private final String refreshCookie;
    private final String csrfCookie;

    ClientType(String refreshCookie, String csrfCookie) {
        this.refreshCookie = refreshCookie;
        this.csrfCookie = csrfCookie;
    }

    public String refreshCookie() {
        return refreshCookie;
    }

    public String csrfCookie() {
        return csrfCookie;
    }
}
