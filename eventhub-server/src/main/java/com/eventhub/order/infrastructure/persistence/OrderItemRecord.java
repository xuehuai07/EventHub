package com.eventhub.order.infrastructure.persistence;

public record OrderItemRecord(
        Long id,
        long orderId,
        long ticketTypeId,
        Long sessionSeatId,
        int quantity,
        long unitPriceCents,
        long subtotalCents,
        String activityTitle,
        String sessionName,
        String venueName,
        String ticketTypeName,
        String areaName,
        String rowLabel,
        String seatNumber) {}
