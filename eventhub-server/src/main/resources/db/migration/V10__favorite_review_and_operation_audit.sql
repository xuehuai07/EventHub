CREATE TABLE eh_activity_favorite (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    activity_id BIGINT NOT NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_eh_activity_favorite_user_activity (user_id, activity_id),
    KEY idx_eh_activity_favorite_user_time (user_id, created_at, id),
    KEY idx_eh_activity_favorite_activity (activity_id),
    CONSTRAINT fk_eh_activity_favorite_user FOREIGN KEY (user_id) REFERENCES eh_user (id),
    CONSTRAINT fk_eh_activity_favorite_activity FOREIGN KEY (activity_id) REFERENCES eh_activity (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE eh_activity_review (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    activity_id BIGINT NOT NULL,
    rating INT NOT NULL,
    content VARCHAR(1000) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PUBLISHED',
    hidden_reason VARCHAR(500) NULL,
    moderator_id BIGINT NULL,
    moderated_at DATETIME(3) NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_eh_activity_review_user_activity (user_id, activity_id),
    KEY idx_eh_activity_review_public (activity_id, status, created_at, id),
    KEY idx_eh_activity_review_moderation (status, created_at, id),
    CONSTRAINT fk_eh_activity_review_user FOREIGN KEY (user_id) REFERENCES eh_user (id),
    CONSTRAINT fk_eh_activity_review_activity FOREIGN KEY (activity_id) REFERENCES eh_activity (id),
    CONSTRAINT fk_eh_activity_review_moderator FOREIGN KEY (moderator_id) REFERENCES eh_user (id),
    CONSTRAINT ck_eh_activity_review_rating CHECK (rating BETWEEN 1 AND 5),
    CONSTRAINT ck_eh_activity_review_status CHECK (status IN ('PUBLISHED', 'HIDDEN'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE eh_operation_log (
    id BIGINT NOT NULL AUTO_INCREMENT,
    operator_user_id BIGINT NOT NULL,
    operator_name VARCHAR(64) NOT NULL,
    operator_role VARCHAR(20) NOT NULL,
    merchant_id BIGINT NULL,
    action VARCHAR(64) NOT NULL,
    resource_type VARCHAR(32) NOT NULL,
    resource_id BIGINT NOT NULL,
    summary VARCHAR(500) NOT NULL,
    request_id VARCHAR(128) NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    KEY idx_eh_operation_log_time (created_at, id),
    KEY idx_eh_operation_log_operator (operator_user_id, created_at),
    KEY idx_eh_operation_log_action (action, created_at),
    KEY idx_eh_operation_log_resource (resource_type, resource_id),
    CONSTRAINT fk_eh_operation_log_operator FOREIGN KEY (operator_user_id) REFERENCES eh_user (id),
    CONSTRAINT ck_eh_operation_log_role CHECK (operator_role IN ('MERCHANT', 'ADMIN'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
