CREATE TABLE eh_user (
    id BIGINT NOT NULL AUTO_INCREMENT,
    username VARCHAR(32) NULL,
    phone VARCHAR(20) NULL,
    password_hash VARCHAR(100) NOT NULL,
    display_name VARCHAR(64) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_eh_user_username (username),
    UNIQUE KEY uk_eh_user_phone (phone),
    CONSTRAINT ck_eh_user_identifier CHECK (username IS NOT NULL OR phone IS NOT NULL)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE eh_role (
    id BIGINT NOT NULL AUTO_INCREMENT,
    code VARCHAR(32) NOT NULL,
    name VARCHAR(64) NOT NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_eh_role_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE eh_permission (
    id BIGINT NOT NULL AUTO_INCREMENT,
    code VARCHAR(64) NOT NULL,
    name VARCHAR(64) NOT NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_eh_permission_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE eh_user_role (
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_eh_user_role_user FOREIGN KEY (user_id) REFERENCES eh_user (id),
    CONSTRAINT fk_eh_user_role_role FOREIGN KEY (role_id) REFERENCES eh_role (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE eh_role_permission (
    role_id BIGINT NOT NULL,
    permission_id BIGINT NOT NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (role_id, permission_id),
    CONSTRAINT fk_eh_role_permission_role FOREIGN KEY (role_id) REFERENCES eh_role (id),
    CONSTRAINT fk_eh_role_permission_permission FOREIGN KEY (permission_id) REFERENCES eh_permission (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE eh_merchant (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE eh_merchant_staff (
    merchant_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    staff_role VARCHAR(32) NOT NULL DEFAULT 'OPERATOR',
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (merchant_id, user_id),
    UNIQUE KEY uk_eh_merchant_staff_user (user_id),
    CONSTRAINT fk_eh_merchant_staff_merchant FOREIGN KEY (merchant_id) REFERENCES eh_merchant (id),
    CONSTRAINT fk_eh_merchant_staff_user FOREIGN KEY (user_id) REFERENCES eh_user (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

INSERT INTO eh_role (code, name)
VALUES ('USER', '普通用户'),
       ('MERCHANT', '商家'),
       ('ADMIN', '平台管理员');

INSERT INTO eh_permission (code, name)
VALUES ('PROFILE_READ', '读取个人资料'),
       ('MERCHANT_ACCESS', '访问商家后台'),
       ('ADMIN_ACCESS', '访问平台管理后台');

INSERT INTO eh_role_permission (role_id, permission_id)
SELECT role.id, permission.id
FROM eh_role role
JOIN eh_permission permission ON permission.code = 'PROFILE_READ'
WHERE role.code IN ('USER', 'MERCHANT', 'ADMIN');

INSERT INTO eh_role_permission (role_id, permission_id)
SELECT role.id, permission.id
FROM eh_role role
JOIN eh_permission permission ON permission.code = 'MERCHANT_ACCESS'
WHERE role.code IN ('MERCHANT', 'ADMIN');

INSERT INTO eh_role_permission (role_id, permission_id)
SELECT role.id, permission.id
FROM eh_role role
JOIN eh_permission permission ON permission.code = 'ADMIN_ACCESS'
WHERE role.code = 'ADMIN';
