ALTER TABLE eh_ticket_order_item
    ADD KEY idx_eh_ticket_order_item_seat (session_seat_id);

ALTER TABLE eh_ticket_order_item
    DROP INDEX uk_eh_ticket_order_item_seat;
