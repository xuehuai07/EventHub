package com.eventhub.order.infrastructure.messaging;

import com.eventhub.order.infrastructure.outbox.OrderPaidEvent;
import com.eventhub.ticket.TicketGenerationService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class OrderPaidConsumer {

    private final TicketGenerationService tickets;

    public OrderPaidConsumer(TicketGenerationService tickets) {
        this.tickets = tickets;
    }

    @RabbitListener(
            queues = OrderMessagingTopology.PAID_QUEUE,
            containerFactory = "orderRabbitListenerContainerFactory")
    public void consume(OrderPaidEvent event) {
        tickets.generate(event);
    }
}
