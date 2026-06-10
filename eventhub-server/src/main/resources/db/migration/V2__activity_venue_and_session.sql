ALTER TABLE eh_merchant
    ADD COLUMN description VARCHAR(500) NULL AFTER name;

INSERT INTO eh_permission (code, name)
VALUES ('ACTIVITY_MANAGE', '管理活动'),
       ('ACTIVITY_REVIEW', '审核活动'),
       ('VENUE_MANAGE', '管理场馆');

INSERT INTO eh_role_permission (role_id, permission_id)
SELECT role.id, permission.id
FROM eh_role role
JOIN eh_permission permission ON permission.code IN ('ACTIVITY_MANAGE', 'VENUE_MANAGE')
WHERE role.code = 'MERCHANT';

INSERT INTO eh_role_permission (role_id, permission_id)
SELECT role.id, permission.id
FROM eh_role role
JOIN eh_permission permission ON permission.code = 'ACTIVITY_REVIEW'
WHERE role.code = 'ADMIN';

CREATE TABLE eh_activity_category (
    id BIGINT NOT NULL AUTO_INCREMENT,
    code VARCHAR(32) NOT NULL,
    name VARCHAR(64) NOT NULL,
    sort_order INT NOT NULL DEFAULT 0,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_eh_activity_category_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE eh_activity_tag (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(32) NOT NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_eh_activity_tag_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE eh_venue (
    id BIGINT NOT NULL AUTO_INCREMENT,
    merchant_id BIGINT NOT NULL,
    name VARCHAR(100) NOT NULL,
    city VARCHAR(64) NOT NULL,
    address VARCHAR(255) NOT NULL,
    seat_mode VARCHAR(20) NOT NULL,
    capacity INT NOT NULL DEFAULT 0,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    version INT NOT NULL DEFAULT 0,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    KEY idx_eh_venue_merchant_status (merchant_id, status),
    CONSTRAINT fk_eh_venue_merchant FOREIGN KEY (merchant_id) REFERENCES eh_merchant (id),
    CONSTRAINT ck_eh_venue_seat_mode CHECK (seat_mode IN ('GENERAL', 'FIXED')),
    CONSTRAINT ck_eh_venue_capacity CHECK (capacity >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE eh_venue_seat (
    id BIGINT NOT NULL AUTO_INCREMENT,
    venue_id BIGINT NOT NULL,
    area_name VARCHAR(64) NOT NULL,
    row_label VARCHAR(20) NOT NULL,
    seat_number VARCHAR(20) NOT NULL,
    seat_code VARCHAR(64) NOT NULL,
    seat_grade VARCHAR(32) NOT NULL DEFAULT 'STANDARD',
    sort_order INT NOT NULL DEFAULT 0,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_eh_venue_seat_code (venue_id, seat_code),
    KEY idx_eh_venue_seat_area (venue_id, area_name, row_label, sort_order),
    CONSTRAINT fk_eh_venue_seat_venue FOREIGN KEY (venue_id) REFERENCES eh_venue (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE eh_activity (
    id BIGINT NOT NULL AUTO_INCREMENT,
    merchant_id BIGINT NOT NULL,
    category_id BIGINT NOT NULL,
    title VARCHAR(120) NOT NULL,
    summary VARCHAR(300) NOT NULL,
    description TEXT NOT NULL,
    cover_url VARCHAR(500) NULL,
    city VARCHAR(64) NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'DRAFT',
    review_reason VARCHAR(500) NULL,
    reviewer_id BIGINT NULL,
    reviewed_at DATETIME(3) NULL,
    published_at DATETIME(3) NULL,
    version INT NOT NULL DEFAULT 0,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    KEY idx_eh_activity_merchant_status (merchant_id, status, updated_at),
    KEY idx_eh_activity_public (status, city, category_id, published_at),
    CONSTRAINT fk_eh_activity_merchant FOREIGN KEY (merchant_id) REFERENCES eh_merchant (id),
    CONSTRAINT fk_eh_activity_category FOREIGN KEY (category_id) REFERENCES eh_activity_category (id),
    CONSTRAINT fk_eh_activity_reviewer FOREIGN KEY (reviewer_id) REFERENCES eh_user (id),
    CONSTRAINT ck_eh_activity_status CHECK (
        status IN ('DRAFT', 'PENDING_REVIEW', 'PUBLISHED', 'REJECTED', 'OFF_SHELF', 'FINISHED')
    )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE eh_activity_tag_relation (
    activity_id BIGINT NOT NULL,
    tag_id BIGINT NOT NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (activity_id, tag_id),
    CONSTRAINT fk_eh_activity_tag_relation_activity FOREIGN KEY (activity_id) REFERENCES eh_activity (id),
    CONSTRAINT fk_eh_activity_tag_relation_tag FOREIGN KEY (tag_id) REFERENCES eh_activity_tag (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE eh_activity_session (
    id BIGINT NOT NULL AUTO_INCREMENT,
    activity_id BIGINT NOT NULL,
    venue_id BIGINT NOT NULL,
    name VARCHAR(100) NOT NULL,
    start_at DATETIME(3) NOT NULL,
    end_at DATETIME(3) NOT NULL,
    sale_start_at DATETIME(3) NOT NULL,
    sale_end_at DATETIME(3) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'SCHEDULED',
    version INT NOT NULL DEFAULT 0,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    KEY idx_eh_activity_session_activity (activity_id, start_at),
    KEY idx_eh_activity_session_venue (venue_id, start_at),
    CONSTRAINT fk_eh_activity_session_activity FOREIGN KEY (activity_id) REFERENCES eh_activity (id),
    CONSTRAINT fk_eh_activity_session_venue FOREIGN KEY (venue_id) REFERENCES eh_venue (id),
    CONSTRAINT ck_eh_activity_session_status CHECK (status IN ('SCHEDULED', 'CANCELLED', 'FINISHED')),
    CONSTRAINT ck_eh_activity_session_time CHECK (start_at < end_at),
    CONSTRAINT ck_eh_activity_session_sale_time CHECK (sale_start_at < sale_end_at AND sale_end_at <= start_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE eh_session_ticket_type (
    id BIGINT NOT NULL AUTO_INCREMENT,
    session_id BIGINT NOT NULL,
    name VARCHAR(64) NOT NULL,
    seat_grade VARCHAR(32) NULL,
    price_cents BIGINT NOT NULL,
    total_stock INT NOT NULL,
    available_stock INT NOT NULL,
    sale_limit_per_user INT NOT NULL DEFAULT 6,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_eh_session_ticket_type_name (session_id, name),
    CONSTRAINT fk_eh_session_ticket_type_session FOREIGN KEY (session_id) REFERENCES eh_activity_session (id),
    CONSTRAINT ck_eh_session_ticket_type_price CHECK (price_cents >= 0),
    CONSTRAINT ck_eh_session_ticket_type_stock CHECK (
        total_stock >= 0 AND available_stock >= 0 AND available_stock <= total_stock
    ),
    CONSTRAINT ck_eh_session_ticket_type_limit CHECK (sale_limit_per_user > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE eh_session_seat (
    id BIGINT NOT NULL AUTO_INCREMENT,
    session_id BIGINT NOT NULL,
    venue_seat_id BIGINT NOT NULL,
    ticket_type_id BIGINT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_eh_session_seat (session_id, venue_seat_id),
    KEY idx_eh_session_seat_status (session_id, status),
    CONSTRAINT fk_eh_session_seat_session FOREIGN KEY (session_id) REFERENCES eh_activity_session (id),
    CONSTRAINT fk_eh_session_seat_venue_seat FOREIGN KEY (venue_seat_id) REFERENCES eh_venue_seat (id),
    CONSTRAINT fk_eh_session_seat_ticket_type FOREIGN KEY (ticket_type_id) REFERENCES eh_session_ticket_type (id),
    CONSTRAINT ck_eh_session_seat_status CHECK (status IN ('AVAILABLE', 'LOCKED', 'SOLD', 'DISABLED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

INSERT INTO eh_activity_category (code, name, sort_order)
VALUES ('MUSIC', '音乐演出', 10),
       ('EXHIBITION', '展览', 20),
       ('THEATRE', '戏剧舞台', 30),
       ('SPORTS', '体育赛事', 40),
       ('LECTURE', '讲座沙龙', 50),
       ('COMMUNITY', '城市社区', 60);
