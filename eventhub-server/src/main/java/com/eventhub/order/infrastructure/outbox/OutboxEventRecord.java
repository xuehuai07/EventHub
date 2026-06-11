package com.eventhub.order.infrastructure.outbox;

import java.time.LocalDateTime;

public record OutboxEventRecord(
        long id,
        String eventId,
        String aggregateType,
        long aggregateId,
        OrderEventType eventType,
        String payload,
        String status,
        int retryCount,
        LocalDateTime nextAttemptAt) {}
