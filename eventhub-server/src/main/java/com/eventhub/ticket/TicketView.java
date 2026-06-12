package com.eventhub.ticket;

import java.time.LocalDateTime;

public record TicketView(
        long id,
        String ticketNo,
        long orderId,
        String orderNo,
        String status,
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
        LocalDateTime usedAt,
        String verifierName) {

    static TicketView from(TicketRecord ticket) {
        return new TicketView(
                ticket.id(),
                ticket.ticketNo(),
                ticket.orderId(),
                ticket.orderNo(),
                ticket.status(),
                ticket.activityTitle(),
                ticket.sessionName(),
                ticket.venueName(),
                ticket.ticketTypeName(),
                ticket.areaName(),
                ticket.rowLabel(),
                ticket.seatNumber(),
                ticket.startAt(),
                ticket.coverUrl(),
                ticket.userDisplayName(),
                ticket.usedAt(),
                ticket.verifierName());
    }
}
