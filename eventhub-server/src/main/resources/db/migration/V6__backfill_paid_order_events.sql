INSERT INTO eh_outbox_event
    (event_id, aggregate_type, aggregate_id, event_type, payload)
SELECT source.event_id,
       'ORDER',
       source.order_id,
       'ORDER_PAID',
       JSON_OBJECT(
           'eventId', source.event_id,
           'orderId', source.order_id,
           'orderNo', source.order_no,
           'paidAt', DATE_FORMAT(source.paid_at, '%Y-%m-%dT%H:%i:%s.%f')
       )
FROM (
    SELECT orders.id AS order_id,
           orders.order_no,
           orders.paid_at,
           UUID() AS event_id
    FROM eh_ticket_order orders
    WHERE orders.status = 'PAID'
      AND orders.paid_at IS NOT NULL
      AND NOT EXISTS (
          SELECT 1
          FROM eh_ticket tickets
          WHERE tickets.order_id = orders.id
      )
      AND NOT EXISTS (
          SELECT 1
          FROM eh_outbox_event events
          WHERE events.aggregate_type = 'ORDER'
            AND events.aggregate_id = orders.id
            AND events.event_type = 'ORDER_PAID'
      )
) source;
