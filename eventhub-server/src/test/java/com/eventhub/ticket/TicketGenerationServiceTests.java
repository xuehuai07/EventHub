package com.eventhub.ticket;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.eventhub.notification.NotificationService;
import com.eventhub.order.domain.order.OrderStatus;
import com.eventhub.order.infrastructure.messaging.MessageConsumeMapper;
import com.eventhub.order.infrastructure.outbox.OrderPaidEvent;
import com.eventhub.order.infrastructure.persistence.OrderItemRecord;
import com.eventhub.order.infrastructure.persistence.OrderQueryMapper;
import com.eventhub.order.infrastructure.persistence.OrderRecord;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TicketGenerationServiceTests {

    @Mock
    private MessageConsumeMapper consumed;

    @Mock
    private OrderQueryMapper orders;

    @Mock
    private TicketMapper tickets;

    @Mock
    private NotificationService notifications;

    @InjectMocks
    private TicketGenerationService service;

    @Test
    void generatesOneTicketPerPurchasedUnit() {
        OrderPaidEvent event = event();
        OrderRecord order = order(OrderStatus.PAID);
        when(consumed.tryRecord("ticket-generation", event.eventId())).thenReturn(1);
        when(orders.findById(event.orderId())).thenReturn(order);
        when(orders.findItems(event.orderId())).thenReturn(List.of(item(10L, 2), item(11L, 1)));

        service.generate(event);

        verify(tickets, times(2))
                .insert(anyString(), eq(42L), eq(10L), org.mockito.ArgumentMatchers.anyInt(), eq(7L), eq(8L), eq(9L));
        verify(tickets).insert(anyString(), eq(42L), eq(11L), eq(1), eq(7L), eq(8L), eq(9L));
    }

    @Test
    void ignoresAlreadyConsumedEvent() {
        OrderPaidEvent event = event();
        when(consumed.tryRecord("ticket-generation", event.eventId())).thenReturn(0);

        service.generate(event);

        verify(orders, never()).findById(event.orderId());
        verify(tickets, never()).insert(anyString(), eq(42L), eq(10L), eq(1), eq(7L), eq(8L), eq(9L));
    }

    @Test
    void rejectsEventForUnpaidOrder() {
        OrderPaidEvent event = event();
        when(consumed.tryRecord("ticket-generation", event.eventId())).thenReturn(1);
        when(orders.findById(event.orderId())).thenReturn(order(OrderStatus.PENDING_PAYMENT));

        assertThrows(IllegalStateException.class, () -> service.generate(event));
    }

    private OrderPaidEvent event() {
        return new OrderPaidEvent("event-2", 42L, "EH42", LocalDateTime.now());
    }

    private OrderRecord order(OrderStatus status) {
        return new OrderRecord(
                42L,
                "EH42",
                "request-42",
                7L,
                1L,
                8L,
                9L,
                5L,
                status,
                1000L,
                3,
                LocalDateTime.now().plusMinutes(15),
                status == OrderStatus.PAID ? LocalDateTime.now() : null,
                LocalDateTime.now(),
                "活动",
                "场次",
                "场馆");
    }

    private OrderItemRecord item(long id, int quantity) {
        return new OrderItemRecord(
                id, 42L, 3L, null, quantity, 1000L, 1000L * quantity, "活动", "场次", "场馆", "标准票", null, null, null);
    }
}
