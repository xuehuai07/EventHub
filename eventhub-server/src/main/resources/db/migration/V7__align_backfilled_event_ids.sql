UPDATE eh_message_consume_record consumed
JOIN eh_outbox_event events
  ON consumed.event_id = JSON_UNQUOTE(JSON_EXTRACT(events.payload, '$.eventId'))
SET consumed.event_id = events.event_id
WHERE consumed.consumer_name = 'ticket-generation'
  AND events.event_type = 'ORDER_PAID'
  AND consumed.event_id <> events.event_id;

UPDATE eh_outbox_event
SET payload = JSON_SET(payload, '$.eventId', event_id)
WHERE event_type = 'ORDER_PAID'
  AND JSON_UNQUOTE(JSON_EXTRACT(payload, '$.eventId')) <> event_id;
