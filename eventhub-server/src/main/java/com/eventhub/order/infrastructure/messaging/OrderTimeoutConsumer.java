package com.eventhub.order.infrastructure.messaging;

import com.eventhub.order.application.timeout.OrderTimeoutMessageService;
import com.eventhub.order.infrastructure.outbox.OrderCreatedEvent;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class OrderTimeoutConsumer {

    private final OrderTimeoutMessageService service;

    public OrderTimeoutConsumer(OrderTimeoutMessageService service) {
        this.service = service;
    }

    @RabbitListener(
            queues = OrderMessagingTopology.TIMEOUT_QUEUE,
            containerFactory = "orderRabbitListenerContainerFactory")
    public void consume(OrderCreatedEvent event) {
        service.handle(event);
    }
}
