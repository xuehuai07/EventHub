package com.eventhub.order.application.order;

import com.eventhub.order.dto.OrderView;
import com.eventhub.order.dto.OrderView.OrderItemView;
import com.eventhub.order.infrastructure.persistence.OrderItemRecord;
import com.eventhub.order.infrastructure.persistence.OrderQueryMapper;
import com.eventhub.order.infrastructure.persistence.OrderRecord;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class OrderViewAssembler {

    private final OrderQueryMapper queries;

    public OrderViewAssembler(OrderQueryMapper queries) {
        this.queries = queries;
    }

    public OrderView view(OrderRecord order) {
        List<OrderItemView> items =
                queries.findItems(order.id()).stream().map(this::item).toList();
        return new OrderView(
                order.id(),
                order.orderNo(),
                order.status(),
                order.totalAmountCents(),
                order.totalQuantity(),
                order.activityTitle(),
                order.sessionName(),
                order.venueName(),
                order.paymentDeadlineAt(),
                order.paidAt(),
                order.createdAt(),
                items);
    }

    public List<OrderView> summaries(List<OrderRecord> orders) {
        return orders.stream()
                .map(order -> new OrderView(
                        order.id(),
                        order.orderNo(),
                        order.status(),
                        order.totalAmountCents(),
                        order.totalQuantity(),
                        order.activityTitle(),
                        order.sessionName(),
                        order.venueName(),
                        order.paymentDeadlineAt(),
                        order.paidAt(),
                        order.createdAt(),
                        List.of()))
                .toList();
    }

    private OrderItemView item(OrderItemRecord item) {
        return new OrderItemView(
                item.id(),
                item.ticketTypeId(),
                item.sessionSeatId(),
                item.quantity(),
                item.unitPriceCents(),
                item.subtotalCents(),
                item.ticketTypeName(),
                item.areaName(),
                item.rowLabel(),
                item.seatNumber());
    }
}
