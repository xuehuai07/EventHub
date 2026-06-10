package com.eventhub.user;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("eventhub.bootstrap")
public record BootstrapIdentityProperties(
        String adminUsername,
        String adminPassword,
        String adminDisplayName,
        String merchantUsername,
        String merchantPassword,
        String merchantDisplayName,
        String merchantName) {}
