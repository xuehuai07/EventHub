package com.eventhub.assistant;

import java.net.URI;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("eventhub.assistant")
public record AssistantProperties(String apiKey, URI baseUrl, String model, Duration timeout) {

    public boolean configured() {
        return apiKey != null && !apiKey.isBlank();
    }
}
