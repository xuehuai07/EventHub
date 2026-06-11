package com.eventhub.ticket;

import com.eventhub.order.domain.order.OrderStatus;
import com.eventhub.order.infrastructure.messaging.MessageConsumeMapper;
import com.eventhub.order.infrastructure.outbox.OrderPaidEvent;
import com.eventhub.order.infrastructure.persistence.OrderItemRecord;
import com.eventhub.order.infrastructure.persistence.OrderQueryMapper;
import com.eventhub.order.infrastructure.persistence.OrderRecord;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TicketGenerationService {

    static final String CONSUMER_NAME = "ticket-generation";

    private final MessageConsumeMapper consumed;
    private final OrderQueryMapper orders;
    private final TicketMapper tickets;

    public TicketGenerationService(MessageConsumeMapper consumed, OrderQueryMapper orders, TicketMapper tickets) {
        this.consumed = consumed;
        this.orders = orders;
        this.tickets = tickets;
    }

    @Transactional
    public void generate(OrderPaidEvent event) {
        if (consumed.tryRecord(CONSUMER_NAME, event.eventId()) == 0) {
            return;
        }
        OrderRecord order = orders.findById(event.orderId());
        if (order == null || order.status() != OrderStatus.PAID) {
            throw new IllegalStateException("Paid order event does not reference a paid order");
        }
        for (OrderItemRecord item : orders.findItems(order.id())) {
            for (int unitNo = 1; unitNo <= item.quantity(); unitNo++) {
                tickets.insert(
                        ticketNo(),
                        order.id(),
                        item.id(),
                        unitNo,
                        order.userId(),
                        order.activityId(),
                        order.sessionId());
            }
        }
    }

    private String ticketNo() {
        return "ET" + UUID.randomUUID().toString().replace("-", "");
    }
}
