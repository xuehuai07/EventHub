CREATE TABLE eh_outbox_event (
    id BIGINT NOT NULL AUTO_INCREMENT,
    event_id CHAR(36) NOT NULL,
    aggregate_type VARCHAR(64) NOT NULL,
    aggregate_id BIGINT NOT NULL,
    event_type VARCHAR(64) NOT NULL,
    payload JSON NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    retry_count INT NOT NULL DEFAULT 0,
    next_attempt_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    published_at DATETIME(3) NULL,
    last_error VARCHAR(1000) NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_eh_outbox_event_id (event_id),
    KEY idx_eh_outbox_publish (status, next_attempt_at, id),
    CONSTRAINT ck_eh_outbox_status CHECK (status IN ('PENDING', 'PUBLISHED', 'FAILED')),
    CONSTRAINT ck_eh_outbox_retry CHECK (retry_count >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE eh_message_consume_record (
    id BIGINT NOT NULL AUTO_INCREMENT,
    consumer_name VARCHAR(100) NOT NULL,
    event_id CHAR(36) NOT NULL,
    consumed_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_eh_message_consume (consumer_name, event_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE eh_ticket (
    id BIGINT NOT NULL AUTO_INCREMENT,
    ticket_no VARCHAR(64) NOT NULL,
    order_id BIGINT NOT NULL,
    order_item_id BIGINT NOT NULL,
    unit_no INT NOT NULL,
    user_id BIGINT NOT NULL,
    activity_id BIGINT NOT NULL,
    session_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'UNUSED',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_eh_ticket_no (ticket_no),
    UNIQUE KEY uk_eh_ticket_item_unit (order_item_id, unit_no),
    KEY idx_eh_ticket_order (order_id),
    KEY idx_eh_ticket_user (user_id, status, created_at),
    KEY idx_eh_ticket_session (session_id, status),
    CONSTRAINT fk_eh_ticket_order FOREIGN KEY (order_id) REFERENCES eh_ticket_order (id),
    CONSTRAINT fk_eh_ticket_order_item FOREIGN KEY (order_item_id) REFERENCES eh_ticket_order_item (id),
    CONSTRAINT fk_eh_ticket_user FOREIGN KEY (user_id) REFERENCES eh_user (id),
    CONSTRAINT fk_eh_ticket_activity FOREIGN KEY (activity_id) REFERENCES eh_activity (id),
    CONSTRAINT fk_eh_ticket_session FOREIGN KEY (session_id) REFERENCES eh_activity_session (id),
    CONSTRAINT ck_eh_ticket_unit CHECK (unit_no > 0),
    CONSTRAINT ck_eh_ticket_status CHECK (status IN ('UNUSED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
