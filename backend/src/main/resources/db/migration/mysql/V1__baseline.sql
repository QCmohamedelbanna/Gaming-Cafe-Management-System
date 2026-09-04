-- Baseline schema for the Gaming Cafe Management System, mirroring the JPA
-- entities under com.cafe.ps.entity as of the SQLite-to-MySQL migration.
-- Table creation order respects foreign-key dependencies.

CREATE TABLE access_rules (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    name          VARCHAR(50)  NOT NULL,
    description   VARCHAR(200),
    system_rule   BIT(1)       NOT NULL,
    system_role   VARCHAR(20),
    created_at    DATETIME(6)  NOT NULL,
    CONSTRAINT uk_access_rule_name UNIQUE (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE devices (
    id                BIGINT AUTO_INCREMENT PRIMARY KEY,
    name              VARCHAR(255) NOT NULL,
    type              VARCHAR(255) NOT NULL,
    status            VARCHAR(255) NOT NULL,
    hourly_rate       DECIMAL(10,2) NOT NULL,
    active            BIT(1),
    deleted           BIT(1),
    maintenance_note  VARCHAR(500),
    CONSTRAINT uk_devices_name UNIQUE (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE products (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    name           VARCHAR(255) NOT NULL,
    price          DECIMAL(10,2) NOT NULL,
    selling_price  DECIMAL(10,2),
    sku            VARCHAR(80),
    category       VARCHAR(80),
    cost_price     DECIMAL(10,2),
    track_stock    BIT(1),
    current_stock  DECIMAL(14,3),
    minimum_stock  DECIMAL(14,3),
    unit           VARCHAR(30),
    active         BIT(1)  NOT NULL,
    deleted        BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT uk_products_name UNIQUE (name),
    CONSTRAINT uk_products_sku  UNIQUE (sku)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE app_users (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    username       VARCHAR(50)  NOT NULL,
    display_name   VARCHAR(100) NOT NULL,
    password_hash  VARCHAR(100) NOT NULL,
    role           VARCHAR(20)  NOT NULL,
    rule_id        BIGINT,
    active         BIT(1)      NOT NULL,
    created_at     DATETIME(6) NOT NULL,
    last_login_at  DATETIME(6),
    CONSTRAINT uk_app_users_username UNIQUE (username),
    CONSTRAINT fk_app_users_rule FOREIGN KEY (rule_id) REFERENCES access_rules (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE pricing (
    id                             BIGINT AUTO_INCREMENT PRIMARY KEY,
    device_type                    VARCHAR(255) NOT NULL,
    session_type                   VARCHAR(255) NOT NULL,
    billing_unit                   VARCHAR(255) NOT NULL,
    price                          DECIMAL(10,2) NOT NULL,
    match_duration_minutes         INT,
    warning_before_expiry_minutes  INT,
    active                         BIT(1) NOT NULL,
    CONSTRAINT uk_pricing_device_session UNIQUE (device_type, session_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE game_sessions (
    id                                        BIGINT AUTO_INCREMENT PRIMARY KEY,
    device_id                                 BIGINT NOT NULL,
    start_time                                DATETIME(6) NOT NULL,
    end_time                                  DATETIME(6),
    planned_minutes                           INT,
    hourly_rate_snapshot                      DECIMAL(10,2) NOT NULL,
    session_type                              VARCHAR(255),
    billing_unit                              VARCHAR(255),
    unit_price_snapshot                       DECIMAL(10,2),
    match_duration_minutes_snapshot           INT,
    purchased_matches                         INT,
    completed_matches                         INT,
    current_match_started_at                  DATETIME(6),
    current_match_expires_at                  DATETIME(6),
    warning_before_expiry_minutes_snapshot    INT,
    match_expired                             BIT(1),
    final_amount                              DECIMAL(10,2),
    status                                     VARCHAR(255) NOT NULL,
    CONSTRAINT fk_game_sessions_device FOREIGN KEY (device_id) REFERENCES devices (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_game_sessions_device_status ON game_sessions (device_id, status);

CREATE TABLE cafe_orders (
    id                BIGINT AUTO_INCREMENT PRIMARY KEY,
    game_session_id   BIGINT,
    created_at        DATETIME(6) NOT NULL,
    completed_at      DATETIME(6),
    status            VARCHAR(255) NOT NULL,
    total_amount      DECIMAL(10,2) NOT NULL,
    discount_amount   DECIMAL(10,2),
    discount_reason   VARCHAR(200),
    CONSTRAINT fk_cafe_orders_session FOREIGN KEY (game_session_id) REFERENCES game_sessions (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_cafe_orders_session_status ON cafe_orders (game_session_id, status);

CREATE TABLE order_items (
    id                    BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id              BIGINT NOT NULL,
    product_id            BIGINT NOT NULL,
    quantity              INT NOT NULL,
    unit_price_snapshot   DECIMAL(10,2) NOT NULL,
    line_total            DECIMAL(10,2) NOT NULL,
    CONSTRAINT fk_order_items_order   FOREIGN KEY (order_id) REFERENCES cafe_orders (id),
    CONSTRAINT fk_order_items_product FOREIGN KEY (product_id) REFERENCES products (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE bills (
    id                       BIGINT AUTO_INCREMENT PRIMARY KEY,
    bill_number              VARCHAR(255) NOT NULL,
    session_id               BIGINT,
    order_id                 BIGINT,
    gaming_amount            DECIMAL(10,2) NOT NULL,
    order_amount             DECIMAL(10,2) NOT NULL,
    total_amount             DECIMAL(10,2) NOT NULL,
    status                   VARCHAR(255) NOT NULL,
    created_at               DATETIME(6) NOT NULL,
    paid_at                  DATETIME(6),
    refunded_at              DATETIME(6),
    automatic_expiry         BIT(1),
    notification_expires_at  DATETIME(6),
    refund_reason            VARCHAR(500),
    CONSTRAINT uk_bills_bill_number UNIQUE (bill_number),
    CONSTRAINT uk_bills_session UNIQUE (session_id),
    CONSTRAINT uk_bills_order   UNIQUE (order_id),
    CONSTRAINT fk_bills_session FOREIGN KEY (session_id) REFERENCES game_sessions (id),
    CONSTRAINT fk_bills_order   FOREIGN KEY (order_id)   REFERENCES cafe_orders (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_bills_status ON bills (status);

CREATE TABLE cashier_shifts (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id          BIGINT NOT NULL,
    opened_at        DATETIME(6) NOT NULL,
    closed_at        DATETIME(6),
    status           VARCHAR(20) NOT NULL,
    opening_cash     DECIMAL(12,2) NOT NULL,
    expected_cash    DECIMAL(12,2),
    actual_cash      DECIMAL(12,2),
    cash_difference  DECIMAL(12,2),
    closing_note     VARCHAR(500),
    CONSTRAINT fk_cashier_shifts_user FOREIGN KEY (user_id) REFERENCES app_users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE payments (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    bill_id          BIGINT NOT NULL,
    shift_id         BIGINT,
    method           VARCHAR(255) NOT NULL,
    amount           DECIMAL(10,2) NOT NULL,
    amount_tendered  DECIMAL(10,2) NOT NULL,
    change_amount    DECIMAL(10,2) NOT NULL,
    status           VARCHAR(255) NOT NULL,
    paid_at          DATETIME(6) NOT NULL,
    cashier          VARCHAR(80),
    CONSTRAINT fk_payments_bill  FOREIGN KEY (bill_id)  REFERENCES bills (id),
    CONSTRAINT fk_payments_shift FOREIGN KEY (shift_id) REFERENCES cashier_shifts (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE stock_movements (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_id  BIGINT NOT NULL,
    type        VARCHAR(20) NOT NULL,
    quantity    DECIMAL(14,3) NOT NULL,
    unit_cost   DECIMAL(10,2),
    reference   VARCHAR(160),
    created_at  DATETIME(6) NOT NULL,
    created_by  VARCHAR(80) NOT NULL,
    CONSTRAINT fk_stock_movements_product FOREIGN KEY (product_id) REFERENCES products (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_stock_movement_product_created ON stock_movements (product_id, created_at);
CREATE INDEX idx_stock_movement_reference ON stock_movements (reference);

CREATE TABLE role_permissions (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    role_name   VARCHAR(20) NOT NULL,
    permission  VARCHAR(60) NOT NULL,
    CONSTRAINT uk_role_permission UNIQUE (role_name, permission)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE rule_permissions (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    rule_id     BIGINT NOT NULL,
    permission  VARCHAR(60) NOT NULL,
    CONSTRAINT uk_rule_permission UNIQUE (rule_id, permission),
    CONSTRAINT fk_rule_permissions_rule FOREIGN KEY (rule_id) REFERENCES access_rules (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
