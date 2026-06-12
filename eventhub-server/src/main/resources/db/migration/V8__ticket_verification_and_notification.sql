ALTER TABLE eh_ticket
    DROP CHECK ck_eh_ticket_status,
    ADD COLUMN used_at DATETIME(3) NULL AFTER status,
    ADD COLUMN verified_by BIGINT NULL AFTER used_at,
    ADD COLUMN verification_device VARCHAR(128) NULL AFTER verified_by,
    ADD KEY idx_eh_ticket_verification (status, used_at),
    ADD CONSTRAINT fk_eh_ticket_verified_by FOREIGN KEY (verified_by) REFERENCES eh_user (id),
    ADD CONSTRAINT ck_eh_ticket_status CHECK (status IN ('UNUSED', 'USED', 'CANCELLED'));

CREATE TABLE eh_ticket_verification_log (
    id BIGINT NOT NULL AUTO_INCREMENT,
    ticket_id BIGINT NOT NULL,
    merchant_id BIGINT NOT NULL,
    operator_id BIGINT NOT NULL,
    result VARCHAR(32) NOT NULL,
    device_id VARCHAR(128) NULL,
    request_ip VARCHAR(64) NULL,
    verified_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    KEY idx_eh_verification_merchant_time (merchant_id, verified_at),
    KEY idx_eh_verification_ticket_time (ticket_id, verified_at),
    CONSTRAINT fk_eh_verification_ticket FOREIGN KEY (ticket_id) REFERENCES eh_ticket (id),
    CONSTRAINT fk_eh_verification_merchant FOREIGN KEY (merchant_id) REFERENCES eh_merchant (id),
    CONSTRAINT fk_eh_verification_operator FOREIGN KEY (operator_id) REFERENCES eh_user (id),
    CONSTRAINT ck_eh_verification_result CHECK (result IN ('SUCCESS', 'ALREADY_USED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE eh_notification (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    type VARCHAR(32) NOT NULL,
    title VARCHAR(120) NOT NULL,
    content VARCHAR(500) NOT NULL,
    resource_type VARCHAR(32) NULL,
    resource_id BIGINT NULL,
    read_at DATETIME(3) NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    KEY idx_eh_notification_user_read (user_id, read_at, created_at),
    CONSTRAINT fk_eh_notification_user FOREIGN KEY (user_id) REFERENCES eh_user (id),
    CONSTRAINT ck_eh_notification_type CHECK (
        type IN ('ORDER_PAID', 'ORDER_CANCELLED', 'ORDER_EXPIRED', 'TICKET_ISSUED')
    )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
