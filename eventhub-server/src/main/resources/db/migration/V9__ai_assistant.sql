CREATE TABLE eh_ai_conversation (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    title VARCHAR(100) NOT NULL,
    last_message_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    KEY idx_eh_ai_conversation_user_time (user_id, last_message_at, id),
    CONSTRAINT fk_eh_ai_conversation_user FOREIGN KEY (user_id) REFERENCES eh_user (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE eh_ai_message (
    id BIGINT NOT NULL AUTO_INCREMENT,
    conversation_id BIGINT NOT NULL,
    role VARCHAR(16) NOT NULL,
    content TEXT NOT NULL,
    resources_json JSON NULL,
    model VARCHAR(64) NULL,
    prompt_tokens INT NULL,
    completion_tokens INT NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    KEY idx_eh_ai_message_conversation (conversation_id, id),
    CONSTRAINT fk_eh_ai_message_conversation FOREIGN KEY (conversation_id)
        REFERENCES eh_ai_conversation (id) ON DELETE CASCADE,
    CONSTRAINT ck_eh_ai_message_role CHECK (role IN ('USER', 'ASSISTANT'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
