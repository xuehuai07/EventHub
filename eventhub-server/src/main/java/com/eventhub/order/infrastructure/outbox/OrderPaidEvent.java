package com.eventhub.order.infrastructure.outbox;

import java.time.LocalDateTime;

public record OrderPaidEvent(String eventId, long orderId, String orderNo, LocalDateTime paidAt) {}
