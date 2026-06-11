package com.eventhub.order;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.eventhub.order.domain.order.OrderStatus;
import com.eventhub.order.infrastructure.outbox.OrderOutboxService;
import com.eventhub.order.infrastructure.outbox.OrderPaidEvent;
import com.eventhub.order.infrastructure.outbox.OutboxEventMapper;
import com.eventhub.order.infrastructure.persistence.OrderRecord;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OrderOutboxServiceTests {

    @Mock
    private OutboxEventMapper events;

    @Mock
    private ObjectMapper objectMapper;

    @Test
    void paidEventUsesPersistedPaymentTime() throws Exception {
        LocalDateTime paidAt = LocalDateTime.of(2026, 6, 11, 16, 30, 45, 123_000_000);
        OrderRecord order = new OrderRecord(
                42L,
                "EH42",
                "request-42",
                7L,
                1L,
                8L,
                9L,
                5L,
                OrderStatus.PAID,
                1000L,
                1,
                paidAt.plusMinutes(15),
                paidAt,
                paidAt.minusMinutes(1),
                "活动",
                "场次",
                "场馆");
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");
        OrderOutboxService service = new OrderOutboxService(events, objectMapper);

        service.appendPaid(order);

        ArgumentCaptor<Object> payload = ArgumentCaptor.forClass(Object.class);
        verify(objectMapper).writeValueAsString(payload.capture());
        assertEquals(paidAt, ((OrderPaidEvent) payload.getValue()).paidAt());
    }
}
