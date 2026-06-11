package com.eventhub.order;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.eventhub.order.application.payment.OrderActionService;
import com.eventhub.order.application.timeout.OrderTimeoutMessageService;
import com.eventhub.order.infrastructure.messaging.MessageConsumeMapper;
import com.eventhub.order.infrastructure.outbox.OrderCreatedEvent;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OrderTimeoutMessageServiceTests {

    @Mock
    private MessageConsumeMapper consumed;

    @Mock
    private OrderActionService actions;

    @InjectMocks
    private OrderTimeoutMessageService service;

    @Test
    void closesOrderOnceForNewEvent() {
        OrderCreatedEvent event = event();
        when(consumed.tryRecord("order-timeout", event.eventId())).thenReturn(1);

        service.handle(event);

        verify(actions).closeExpired(event.orderId());
    }

    @Test
    void ignoresAlreadyConsumedEvent() {
        OrderCreatedEvent event = event();
        when(consumed.tryRecord("order-timeout", event.eventId())).thenReturn(0);

        service.handle(event);

        verify(actions, never()).closeExpired(event.orderId());
    }

    private OrderCreatedEvent event() {
        return new OrderCreatedEvent("event-1", 42L, "EH42", LocalDateTime.now());
    }
}
