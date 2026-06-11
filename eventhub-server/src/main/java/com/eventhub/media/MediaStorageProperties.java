package com.eventhub.media;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("eventhub.media")
public record MediaStorageProperties(String uploadRoot) {}
