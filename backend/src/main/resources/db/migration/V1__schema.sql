CREATE TABLE `user` (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_code CHAR(6) NOT NULL,
    phone VARCHAR(11) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    nickname VARCHAR(20) NOT NULL,
    avatar_url VARCHAR(255) DEFAULT NULL,
    address VARCHAR(100) DEFAULT NULL,
    points_balance INT NOT NULL DEFAULT 0,
    token_version INT NOT NULL DEFAULT 0,
    must_change_password TINYINT(1) NOT NULL DEFAULT 0,
    status VARCHAR(10) NOT NULL DEFAULT 'ACTIVE',
    disabled_reason VARCHAR(200) DEFAULT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_code (user_code),
    UNIQUE KEY uk_user_phone (phone),
    CONSTRAINT chk_user_points CHECK (points_balance >= 0)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE recycler (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(20) NOT NULL,
    phone VARCHAR(11) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    employee_no VARCHAR(20) NOT NULL,
    site_id BIGINT DEFAULT NULL,
    status VARCHAR(10) NOT NULL DEFAULT 'PENDING',
    token_version INT NOT NULL DEFAULT 0,
    must_change_password TINYINT(1) NOT NULL DEFAULT 0,
    apply_reason VARCHAR(200) DEFAULT NULL,
    reject_reason VARCHAR(200) DEFAULT NULL,
    approved_by BIGINT DEFAULT NULL,
    approved_at DATETIME DEFAULT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_recycler_phone (phone),
    KEY idx_recycler_site (site_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE admin_user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(30) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    status VARCHAR(10) NOT NULL DEFAULT 'ACTIVE',
    token_version INT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_admin_username (username)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE site (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(30) NOT NULL,
    address VARCHAR(100) NOT NULL,
    open_time VARCHAR(50) DEFAULT NULL,
    manager VARCHAR(20) DEFAULT NULL,
    phone VARCHAR(20) DEFAULT NULL,
    status VARCHAR(10) NOT NULL DEFAULT 'ENABLED'
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE recycle_category (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(20) NOT NULL,
    code VARCHAR(20) NOT NULL,
    unit VARCHAR(10) NOT NULL DEFAULT 'kg',
    icon_url VARCHAR(255) DEFAULT NULL,
    sort INT NOT NULL DEFAULT 0,
    status VARCHAR(10) NOT NULL DEFAULT 'ENABLED',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_category_code (code)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE points_rule (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    category_id BIGINT NOT NULL,
    points_per_kg INT NOT NULL,
    effective_from DATETIME NOT NULL,
    created_by BIGINT DEFAULT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_category_effective (category_id, effective_from)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE delivery_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    record_no VARCHAR(24) NOT NULL,
    user_id BIGINT NOT NULL,
    recycler_id BIGINT NOT NULL,
    site_id BIGINT DEFAULT NULL,
    category_id BIGINT NOT NULL,
    rule_id BIGINT DEFAULT NULL,
    weight DECIMAL(6, 2) NOT NULL,
    points INT NOT NULL,
    status VARCHAR(10) NOT NULL DEFAULT 'NORMAL',
    remark VARCHAR(100) DEFAULT NULL,
    revoke_reason VARCHAR(200) DEFAULT NULL,
    revoked_by_type VARCHAR(10) DEFAULT NULL,
    revoked_by_id BIGINT DEFAULT NULL,
    revoked_at DATETIME DEFAULT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_record_no (record_no),
    KEY idx_user_created (user_id, created_at),
    KEY idx_recycler_created (recycler_id, created_at),
    KEY idx_status (status),
    KEY idx_created_at (created_at),
    CONSTRAINT chk_delivery_weight CHECK (weight > 0)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE points_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    type VARCHAR(10) NOT NULL,
    `change` INT NOT NULL,
    balance_after INT NOT NULL,
    biz_type VARCHAR(20) NOT NULL,
    biz_id BIGINT DEFAULT NULL,
    biz_no VARCHAR(24) DEFAULT NULL,
    operator_type VARCHAR(10) DEFAULT NULL,
    operator_id BIGINT DEFAULT NULL,
    remark VARCHAR(200) DEFAULT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_user_created (user_id, created_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE product (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(50) NOT NULL,
    description VARCHAR(500) DEFAULT NULL,
    cover_url VARCHAR(255) DEFAULT NULL,
    points_price INT NOT NULL,
    stock INT NOT NULL DEFAULT 0,
    sold_count INT NOT NULL DEFAULT 0,
    spec VARCHAR(100) DEFAULT NULL,
    exchange_notice VARCHAR(200) DEFAULT NULL,
    status VARCHAR(10) NOT NULL DEFAULT 'ON',
    version INT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_product_status (status),
    CONSTRAINT chk_product_points CHECK (points_price >= 1),
    CONSTRAINT chk_product_stock CHECK (stock >= 0)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE exchange_order (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_no VARCHAR(24) NOT NULL,
    user_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    quantity INT NOT NULL,
    points_cost INT NOT NULL,
    status VARCHAR(12) NOT NULL DEFAULT 'PENDING',
    redeem_code CHAR(8) NOT NULL,
    redeem_expire_at DATETIME NOT NULL,
    verified_by BIGINT DEFAULT NULL,
    verified_site_id BIGINT DEFAULT NULL,
    verified_at DATETIME DEFAULT NULL,
    cancel_reason VARCHAR(200) DEFAULT NULL,
    cancelled_by BIGINT DEFAULT NULL,
    idempotency_key VARCHAR(64) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_order_no (order_no),
    UNIQUE KEY uk_redeem_code (redeem_code),
    UNIQUE KEY uk_user_idem (user_id, idempotency_key),
    KEY idx_user_created (user_id, created_at),
    KEY idx_status (status),
    KEY idx_created_at (created_at),
    KEY idx_verified_by (verified_by),
    CONSTRAINT chk_order_quantity CHECK (quantity >= 1)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE announcement (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    title VARCHAR(50) NOT NULL,
    type VARCHAR(12) NOT NULL,
    content TEXT NOT NULL,
    cover_url VARCHAR(255) DEFAULT NULL,
    status VARCHAR(12) NOT NULL DEFAULT 'PUBLISHED',
    published_at DATETIME DEFAULT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE appeal (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    target_type VARCHAR(10) NOT NULL,
    target_id BIGINT NOT NULL,
    description VARCHAR(500) NOT NULL,
    status VARCHAR(12) NOT NULL DEFAULT 'PENDING',
    conclusion VARCHAR(500) DEFAULT NULL,
    handled_by BIGINT DEFAULT NULL,
    handled_at DATETIME DEFAULT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_status_created (status, created_at),
    KEY idx_user_created (user_id, created_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE upload_file (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    url VARCHAR(255) NOT NULL,
    storage_path VARCHAR(255) NOT NULL,
    size INT NOT NULL,
    mime VARCHAR(50) NOT NULL,
    scene VARCHAR(20) NOT NULL,
    uploaded_by_type VARCHAR(10) NOT NULL,
    uploaded_by_id BIGINT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_uploader (uploaded_by_type, uploaded_by_id, created_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;
