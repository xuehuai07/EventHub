package com.eventhub.order.infrastructure.persistence;

import com.eventhub.activity.domain.SeatMode;
import java.time.LocalDateTime;

public record SeatLockRecord(
        Long id,
        String lockNo,
        long userId,
        long sessionId,
        long ticketTypeId,
        SeatMode seatMode,
        int quantity,
        String status,
        Long consumedOrderId,
        LocalDateTime expiresAt) {}
