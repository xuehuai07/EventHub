package com.eventhub.order.infrastructure.persistence;

import com.eventhub.order.domain.order.OrderStatus;
import java.time.LocalDateTime;

public record OrderRecord(
        Long id,
        String orderNo,
        String requestId,
        long userId,
        long merchantId,
        long activityId,
        long sessionId,
        long lockId,
        OrderStatus status,
        long totalAmountCents,
        int totalQuantity,
        LocalDateTime paymentDeadlineAt,
        LocalDateTime paidAt,
        LocalDateTime createdAt,
        String activityTitle,
        String sessionName,
        String venueName) {}
