package com.eventhub.order.infrastructure.outbox;

import java.time.LocalDateTime;

public record OrderCreatedEvent(String eventId, long orderId, String orderNo, LocalDateTime paymentDeadlineAt) {}
