package com.eventhub.order.dto;

import com.eventhub.activity.domain.SeatMode;
import java.time.LocalDateTime;
import java.util.List;

public record SeatLockView(
        String lockNo,
        long sessionId,
        long ticketTypeId,
        SeatMode seatMode,
        int quantity,
        long amountCents,
        String status,
        LocalDateTime expiresAt,
        List<Long> sessionSeatIds) {}
