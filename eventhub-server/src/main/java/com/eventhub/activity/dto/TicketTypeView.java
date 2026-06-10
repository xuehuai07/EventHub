package com.eventhub.activity.dto;

public record TicketTypeView(
        long id,
        String name,
        String seatGrade,
        long priceCents,
        int totalStock,
        int availableStock,
        int saleLimitPerUser) {}
