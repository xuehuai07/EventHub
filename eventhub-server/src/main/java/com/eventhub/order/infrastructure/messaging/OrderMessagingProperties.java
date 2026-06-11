package com.eventhub.order.infrastructure.messaging;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("eventhub.order.messaging")
public record OrderMessagingProperties(Duration outboxScanDelay, Duration confirmTimeout, boolean publisherEnabled) {}
