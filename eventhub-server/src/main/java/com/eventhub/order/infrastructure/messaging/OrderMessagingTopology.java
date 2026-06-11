package com.eventhub.order.infrastructure.messaging;

public final class OrderMessagingTopology {

    public static final String EVENTS_EXCHANGE = "eventhub.order.events";
    public static final String DELAY_EXCHANGE = "eventhub.order.delay";
    public static final String DEAD_LETTER_EXCHANGE = "eventhub.order.dlx";

    public static final String TIMEOUT_DELAY_QUEUE = "eventhub.order.timeout.delay.q";
    public static final String TIMEOUT_QUEUE = "eventhub.order.timeout.q";
    public static final String PAID_QUEUE = "eventhub.order.paid.q";
    public static final String DEAD_QUEUE = "eventhub.order.dead.q";

    public static final String TIMEOUT_DELAY_KEY = "order.timeout.delay";
    public static final String TIMEOUT_KEY = "order.timeout";
    public static final String PAID_KEY = "order.paid";
    public static final String TIMEOUT_DEAD_KEY = "order.timeout.dead";
    public static final String PAID_DEAD_KEY = "order.paid.dead";

    private OrderMessagingTopology() {}
}
