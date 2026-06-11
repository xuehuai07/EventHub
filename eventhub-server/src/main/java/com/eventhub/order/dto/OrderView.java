package com.eventhub.order.dto;

import com.eventhub.order.domain.order.OrderStatus;
import java.time.LocalDateTime;
import java.util.List;

public record OrderView(
        long id,
        String orderNo,
        OrderStatus status,
        long totalAmountCents,
        int totalQuantity,
        String activityTitle,
        String sessionName,
        String venueName,
        LocalDateTime paymentDeadlineAt,
        LocalDateTime paidAt,
        LocalDateTime createdAt,
        List<OrderItemView> items) {

    public record OrderItemView(
            long id,
            long ticketTypeId,
            Long sessionSeatId,
            int quantity,
            long unitPriceCents,
            long subtotalCents,
            String ticketTypeName,
            String areaName,
            String rowLabel,
            String seatNumber) {}
}
