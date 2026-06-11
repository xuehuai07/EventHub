package com.eventhub.order.infrastructure.outbox;

import com.eventhub.order.infrastructure.messaging.OrderMessagingProperties;
import com.eventhub.order.infrastructure.messaging.OrderMessagingTopology;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(
        prefix = "eventhub.order.messaging",
        name = "publisher-enabled",
        havingValue = "true",
        matchIfMissing = true)
public class OrderOutboxPublisher {

    private static final Logger log = LoggerFactory.getLogger(OrderOutboxPublisher.class);

    private final OutboxEventMapper events;
    private final ObjectMapper objectMapper;
    private final RabbitTemplate rabbitTemplate;
    private final OrderMessagingProperties properties;

    public OrderOutboxPublisher(
            OutboxEventMapper events,
            ObjectMapper objectMapper,
            RabbitTemplate rabbitTemplate,
            OrderMessagingProperties properties) {
        this.events = events;
        this.objectMapper = objectMapper;
        this.rabbitTemplate = rabbitTemplate;
        this.properties = properties;
    }

    @Scheduled(fixedDelayString = "${eventhub.order.messaging.outbox-scan-delay:5s}")
    public void publishReady() {
        events.findReady(LocalDateTime.now(), 100).forEach(this::publish);
    }

    private void publish(OutboxEventRecord event) {
        try {
            Object payload = payload(event);
            Destination destination = destination(event);
            CorrelationData correlation = new CorrelationData(event.eventId());
            rabbitTemplate.convertAndSend(
                    destination.exchange(),
                    destination.routingKey(),
                    payload,
                    message -> {
                        message.getMessageProperties().setMessageId(event.eventId());
                        message.getMessageProperties().setType(event.eventType().name());
                        message.getMessageProperties().setDeliveryMode(MessageDeliveryMode.PERSISTENT);
                        if (destination.expiration() != null) {
                            message.getMessageProperties()
                                    .setExpiration(Long.toString(
                                            destination.expiration().toMillis()));
                        }
                        return message;
                    },
                    correlation);
            CorrelationData.Confirm confirm =
                    correlation.getFuture().get(properties.confirmTimeout().toMillis(), TimeUnit.MILLISECONDS);
            if (!confirm.isAck()) {
                throw new IllegalStateException("Broker rejected event: " + confirm.getReason());
            }
            if (correlation.getReturned() != null) {
                throw new IllegalStateException("Event was not routed");
            }
            events.markPublished(event.id());
        } catch (Exception exception) {
            log.warn("Failed to publish outbox event {}", event.eventId(), exception);
            events.markFailed(event.id(), nextAttempt(event.retryCount()), shortMessage(exception));
        }
    }

    private Object payload(OutboxEventRecord event) throws JsonProcessingException {
        return switch (event.eventType()) {
            case ORDER_CREATED -> objectMapper.readValue(event.payload(), OrderCreatedEvent.class);
            case ORDER_PAID -> objectMapper.readValue(event.payload(), OrderPaidEvent.class);
        };
    }

    private Destination destination(OutboxEventRecord event) {
        return switch (event.eventType()) {
            case ORDER_CREATED ->
                new Destination(
                        OrderMessagingTopology.DELAY_EXCHANGE,
                        OrderMessagingTopology.TIMEOUT_DELAY_KEY,
                        timeout(event));
            case ORDER_PAID ->
                new Destination(OrderMessagingTopology.EVENTS_EXCHANGE, OrderMessagingTopology.PAID_KEY, null);
        };
    }

    private Duration timeout(OutboxEventRecord event) {
        try {
            OrderCreatedEvent payload = objectMapper.readValue(event.payload(), OrderCreatedEvent.class);
            Duration duration = Duration.between(
                    LocalDateTime.now().atZone(ZoneId.systemDefault()).toInstant(),
                    payload.paymentDeadlineAt().atZone(ZoneId.systemDefault()).toInstant());
            return duration.isNegative() ? Duration.ZERO : duration;
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("Invalid order-created event payload", exception);
        }
    }

    private LocalDateTime nextAttempt(int retryCount) {
        long seconds = Math.min(300, 1L << Math.min(retryCount, 8));
        return LocalDateTime.now().plusSeconds(seconds);
    }

    private String shortMessage(Exception exception) {
        String message = exception.getMessage();
        if (message == null || message.isBlank()) {
            message = exception.getClass().getSimpleName();
        }
        return message.substring(0, Math.min(message.length(), 1000));
    }

    private record Destination(String exchange, String routingKey, Duration expiration) {}
}
