package com.eventhub.order.application.timeout;

import com.eventhub.order.application.payment.OrderActionService;
import com.eventhub.order.infrastructure.persistence.OrderQueryMapper;
import java.time.LocalDateTime;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class OrderTimeoutService {

    private final OrderQueryMapper queries;
    private final OrderActionService actions;

    public OrderTimeoutService(OrderQueryMapper queries, OrderActionService actions) {
        this.queries = queries;
        this.actions = actions;
    }

    @Scheduled(fixedDelayString = "${eventhub.order.timeout-scan-delay:60000}")
    public void closeExpiredOrders() {
        queries.findExpiredOrderIds(LocalDateTime.now(), 100).forEach(actions::closeExpired);
    }
}
