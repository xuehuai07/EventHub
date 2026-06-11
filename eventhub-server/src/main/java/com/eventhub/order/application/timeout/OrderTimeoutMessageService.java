package com.eventhub.order.application.timeout;

import com.eventhub.order.application.payment.OrderActionService;
import com.eventhub.order.infrastructure.messaging.MessageConsumeMapper;
import com.eventhub.order.infrastructure.outbox.OrderCreatedEvent;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderTimeoutMessageService {

    static final String CONSUMER_NAME = "order-timeout";

    private final MessageConsumeMapper consumed;
    private final OrderActionService actions;

    public OrderTimeoutMessageService(MessageConsumeMapper consumed, OrderActionService actions) {
        this.consumed = consumed;
        this.actions = actions;
    }

    @Transactional
    public void handle(OrderCreatedEvent event) {
        if (consumed.tryRecord(CONSUMER_NAME, event.eventId()) == 0) {
            return;
        }
        actions.closeExpired(event.orderId());
    }
}
