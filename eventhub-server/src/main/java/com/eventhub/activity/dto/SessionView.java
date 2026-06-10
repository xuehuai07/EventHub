package com.eventhub.activity.dto;

import com.eventhub.activity.domain.SeatMode;
import java.time.LocalDateTime;
import java.util.List;

public record SessionView(
        long id,
        long venueId,
        String venueName,
        String venueAddress,
        SeatMode seatMode,
        String name,
        LocalDateTime startAt,
        LocalDateTime endAt,
        LocalDateTime saleStartAt,
        LocalDateTime saleEndAt,
        String status,
        int version,
        List<TicketTypeView> ticketTypes,
        List<SeatAreaView> seatAreas) {}
