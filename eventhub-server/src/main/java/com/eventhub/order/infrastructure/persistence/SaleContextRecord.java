package com.eventhub.order.infrastructure.persistence;

import com.eventhub.activity.domain.SeatMode;
import java.time.LocalDateTime;

public record SaleContextRecord(
        long sessionId,
        long activityId,
        long merchantId,
        long venueId,
        long ticketTypeId,
        String activityTitle,
        String sessionName,
        String venueName,
        SeatMode seatMode,
        String ticketTypeName,
        String seatGrade,
        long priceCents,
        int totalStock,
        int availableStock,
        int saleLimitPerUser,
        LocalDateTime saleStartAt,
        LocalDateTime saleEndAt,
        String sessionStatus,
        String activityStatus) {}
