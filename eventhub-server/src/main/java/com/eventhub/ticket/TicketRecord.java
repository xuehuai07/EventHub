package com.eventhub.ticket;

import java.time.LocalDateTime;

public record TicketRecord(
        long id,
        String ticketNo,
        long orderId,
        long userId,
        String status,
        LocalDateTime usedAt,
        Long verifiedBy,
        String verificationDevice,
        String orderNo,
        long merchantId,
        String activityTitle,
        String sessionName,
        String venueName,
        String ticketTypeName,
        String areaName,
        String rowLabel,
        String seatNumber,
        LocalDateTime startAt,
        String coverUrl,
        String userDisplayName,
        String verifierName) {}
