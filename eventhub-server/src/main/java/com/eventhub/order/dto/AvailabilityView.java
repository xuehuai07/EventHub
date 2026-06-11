package com.eventhub.order.dto;

import com.eventhub.activity.domain.SeatMode;
import java.time.LocalDateTime;
import java.util.List;

public record AvailabilityView(
        long sessionId,
        String activityTitle,
        String sessionName,
        String venueName,
        SeatMode seatMode,
        LocalDateTime saleStartAt,
        LocalDateTime saleEndAt,
        List<TicketAvailability> ticketTypes,
        List<SeatAvailability> seats) {

    public record TicketAvailability(
            long id,
            String name,
            String seatGrade,
            long priceCents,
            int totalStock,
            int availableStock,
            int lockedStock,
            int lockableStock,
            int saleLimitPerUser) {}

    public record SeatAvailability(
            long id,
            long ticketTypeId,
            String areaName,
            String rowLabel,
            String seatNumber,
            String seatGrade,
            long priceCents,
            String status) {}
}
