package com.eventhub.activity.infrastructure.persistence;

public record TicketTypeRecord(
        Long id,
        long sessionId,
        String name,
        String seatGrade,
        long priceCents,
        int totalStock,
        int availableStock,
        int saleLimitPerUser) {}
