package com.eventhub.order.infrastructure.persistence;

public record SeatRecord(
        long id,
        long sessionId,
        Long ticketTypeId,
        String areaName,
        String rowLabel,
        String seatNumber,
        String seatGrade,
        String status,
        long priceCents) {}
