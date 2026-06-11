CREATE TABLE eh_seat_lock_record (
    id BIGINT NOT NULL AUTO_INCREMENT,
    lock_no VARCHAR(64) NOT NULL,
    user_id BIGINT NOT NULL,
    session_id BIGINT NOT NULL,
    ticket_type_id BIGINT NOT NULL,
    seat_mode VARCHAR(20) NOT NULL,
    quantity INT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    consumed_order_id BIGINT NULL,
    expires_at DATETIME(3) NOT NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_eh_seat_lock_no (lock_no),
    KEY idx_eh_seat_lock_user (user_id, status, expires_at),
    KEY idx_eh_seat_lock_session (session_id, status, expires_at),
    CONSTRAINT fk_eh_seat_lock_user FOREIGN KEY (user_id) REFERENCES eh_user (id),
    CONSTRAINT fk_eh_seat_lock_session FOREIGN KEY (session_id) REFERENCES eh_activity_session (id),
    CONSTRAINT fk_eh_seat_lock_ticket_type FOREIGN KEY (ticket_type_id) REFERENCES eh_session_ticket_type (id),
    CONSTRAINT ck_eh_seat_lock_mode CHECK (seat_mode IN ('GENERAL', 'FIXED')),
    CONSTRAINT ck_eh_seat_lock_quantity CHECK (quantity > 0),
    CONSTRAINT ck_eh_seat_lock_status CHECK (status IN ('ACTIVE', 'CONSUMED', 'RELEASED', 'EXPIRED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE eh_seat_lock_item (
    id BIGINT NOT NULL AUTO_INCREMENT,
    lock_id BIGINT NOT NULL,
    session_seat_id BIGINT NOT NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_eh_seat_lock_item (lock_id, session_seat_id),
    CONSTRAINT fk_eh_seat_lock_item_lock FOREIGN KEY (lock_id) REFERENCES eh_seat_lock_record (id),
    CONSTRAINT fk_eh_seat_lock_item_seat FOREIGN KEY (session_seat_id) REFERENCES eh_session_seat (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE eh_ticket_order (
    id BIGINT NOT NULL AUTO_INCREMENT,
    order_no VARCHAR(64) NOT NULL,
    request_id VARCHAR(64) NOT NULL,
    user_id BIGINT NOT NULL,
    merchant_id BIGINT NOT NULL,
    activity_id BIGINT NOT NULL,
    session_id BIGINT NOT NULL,
    lock_id BIGINT NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'PENDING_PAYMENT',
    total_amount_cents BIGINT NOT NULL,
    total_quantity INT NOT NULL,
    payment_deadline_at DATETIME(3) NOT NULL,
    paid_at DATETIME(3) NULL,
    cancelled_at DATETIME(3) NULL,
    expired_at DATETIME(3) NULL,
    version INT NOT NULL DEFAULT 0,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_eh_ticket_order_no (order_no),
    UNIQUE KEY uk_eh_ticket_order_request (user_id, request_id),
    UNIQUE KEY uk_eh_ticket_order_lock (lock_id),
    KEY idx_eh_ticket_order_user (user_id, status, created_at),
    KEY idx_eh_ticket_order_merchant (merchant_id, status, created_at),
    KEY idx_eh_ticket_order_timeout (status, payment_deadline_at),
    CONSTRAINT fk_eh_ticket_order_user FOREIGN KEY (user_id) REFERENCES eh_user (id),
    CONSTRAINT fk_eh_ticket_order_merchant FOREIGN KEY (merchant_id) REFERENCES eh_merchant (id),
    CONSTRAINT fk_eh_ticket_order_activity FOREIGN KEY (activity_id) REFERENCES eh_activity (id),
    CONSTRAINT fk_eh_ticket_order_session FOREIGN KEY (session_id) REFERENCES eh_activity_session (id),
    CONSTRAINT fk_eh_ticket_order_lock FOREIGN KEY (lock_id) REFERENCES eh_seat_lock_record (id),
    CONSTRAINT ck_eh_ticket_order_status CHECK (
        status IN ('PENDING_PAYMENT', 'PAID', 'CANCELLED', 'EXPIRED')
    ),
    CONSTRAINT ck_eh_ticket_order_amount CHECK (total_amount_cents >= 0 AND total_quantity > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE eh_ticket_order_item (
    id BIGINT NOT NULL AUTO_INCREMENT,
    order_id BIGINT NOT NULL,
    ticket_type_id BIGINT NOT NULL,
    session_seat_id BIGINT NULL,
    quantity INT NOT NULL,
    unit_price_cents BIGINT NOT NULL,
    subtotal_cents BIGINT NOT NULL,
    activity_title VARCHAR(120) NOT NULL,
    session_name VARCHAR(100) NOT NULL,
    venue_name VARCHAR(100) NOT NULL,
    ticket_type_name VARCHAR(64) NOT NULL,
    area_name VARCHAR(64) NULL,
    row_label VARCHAR(20) NULL,
    seat_number VARCHAR(20) NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_eh_ticket_order_item_seat (session_seat_id),
    KEY idx_eh_ticket_order_item_order (order_id),
    CONSTRAINT fk_eh_ticket_order_item_order FOREIGN KEY (order_id) REFERENCES eh_ticket_order (id),
    CONSTRAINT fk_eh_ticket_order_item_ticket_type FOREIGN KEY (ticket_type_id) REFERENCES eh_session_ticket_type (id),
    CONSTRAINT fk_eh_ticket_order_item_seat FOREIGN KEY (session_seat_id) REFERENCES eh_session_seat (id),
    CONSTRAINT ck_eh_ticket_order_item_quantity CHECK (quantity > 0),
    CONSTRAINT ck_eh_ticket_order_item_amount CHECK (unit_price_cents >= 0 AND subtotal_cents >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE eh_payment_record (
    id BIGINT NOT NULL AUTO_INCREMENT,
    payment_no VARCHAR(64) NOT NULL,
    order_id BIGINT NOT NULL,
    amount_cents BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL,
    paid_at DATETIME(3) NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_eh_payment_no (payment_no),
    UNIQUE KEY uk_eh_payment_order (order_id),
    CONSTRAINT fk_eh_payment_order FOREIGN KEY (order_id) REFERENCES eh_ticket_order (id),
    CONSTRAINT ck_eh_payment_status CHECK (status IN ('SUCCESS', 'FAILED')),
    CONSTRAINT ck_eh_payment_amount CHECK (amount_cents >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE eh_idempotency_record (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    scope VARCHAR(64) NOT NULL,
    idempotency_key VARCHAR(64) NOT NULL,
    request_hash VARCHAR(64) NOT NULL,
    resource_type VARCHAR(32) NOT NULL,
    resource_id BIGINT NOT NULL,
    expires_at DATETIME(3) NOT NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_eh_idempotency (user_id, scope, idempotency_key),
    KEY idx_eh_idempotency_expiry (expires_at),
    CONSTRAINT fk_eh_idempotency_user FOREIGN KEY (user_id) REFERENCES eh_user (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

INSERT INTO eh_session_seat (session_id, venue_seat_id, ticket_type_id, status)
SELECT session.id,
       venue_seat.id,
       ticket_type.id,
       CASE WHEN ticket_type.id IS NULL THEN 'DISABLED' ELSE 'AVAILABLE' END
FROM eh_activity_session session
JOIN eh_venue venue ON venue.id = session.venue_id AND venue.seat_mode = 'FIXED'
JOIN eh_venue_seat venue_seat ON venue_seat.venue_id = venue.id AND venue_seat.status = 'ACTIVE'
LEFT JOIN eh_session_ticket_type ticket_type
       ON ticket_type.session_id = session.id
      AND ticket_type.status = 'ACTIVE'
      AND ticket_type.seat_grade = venue_seat.seat_grade
ON DUPLICATE KEY UPDATE
    ticket_type_id = VALUES(ticket_type_id),
    status = VALUES(status);

UPDATE eh_session_ticket_type ticket_type
JOIN (
    SELECT session_seat.ticket_type_id, COUNT(*) AS seat_count
    FROM eh_session_seat session_seat
    WHERE session_seat.ticket_type_id IS NOT NULL
    GROUP BY session_seat.ticket_type_id
) snapshot ON snapshot.ticket_type_id = ticket_type.id
SET ticket_type.total_stock = snapshot.seat_count,
    ticket_type.available_stock = snapshot.seat_count;
