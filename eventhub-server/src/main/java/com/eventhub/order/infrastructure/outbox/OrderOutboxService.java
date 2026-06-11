package com.eventhub.order.infrastructure.outbox;

import com.eventhub.order.infrastructure.persistence.OrderRecord;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class OrderOutboxService {

    private final OutboxEventMapper events;
    private final ObjectMapper objectMapper;

    public OrderOutboxService(OutboxEventMapper events, ObjectMapper objectMapper) {
        this.events = events;
        this.objectMapper = objectMapper;
    }

    public void appendCreated(OrderRecord order) {
        String eventId = UUID.randomUUID().toString();
        append(
                eventId,
                order.id(),
                OrderEventType.ORDER_CREATED,
                new OrderCreatedEvent(eventId, order.id(), order.orderNo(), order.paymentDeadlineAt()));
    }

    public void appendPaid(OrderRecord order) {
        String eventId = UUID.randomUUID().toString();
        append(
                eventId,
                order.id(),
                OrderEventType.ORDER_PAID,
                new OrderPaidEvent(
                        eventId,
                        order.id(),
                        order.orderNo(),
                        Objects.requireNonNull(order.paidAt(), "Paid order must have paidAt")));
    }

    private void append(String eventId, long orderId, OrderEventType eventType, Object payload) {
        try {
            events.insert(eventId, "ORDER", orderId, eventType.name(), objectMapper.writeValueAsString(payload));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to serialize order event", exception);
        }
    }
}
