-- SQLite baseline for the installed Gaming Cafe application.
--
-- Every statement is idempotent so the legacy backend/ps_cafe.db can acquire
-- Flyway metadata without losing or replacing any existing table or row.
-- Hibernate's non-destructive update mode fills any columns that an older
-- development database does not yet contain.

CREATE TABLE IF NOT EXISTS access_rules (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name VARCHAR(50) NOT NULL,
    description VARCHAR(200),
    system_rule BOOLEAN NOT NULL,
    system_role VARCHAR(20) CHECK (system_role IN ('ADMIN', 'MANAGER', 'CASHIER')),
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT uk_access_rule_name UNIQUE (name)
);

CREATE TABLE IF NOT EXISTS devices (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name VARCHAR(255) NOT NULL,
    type VARCHAR(255) NOT NULL CHECK (type IN ('PS4', 'PS5')),
    status VARCHAR(255) NOT NULL CHECK (status IN ('AVAILABLE', 'PLAYING', 'RESERVED', 'MAINTENANCE', 'OFFLINE')),
    hourly_rate NUMERIC(10, 2) NOT NULL DEFAULT 0,
    active BOOLEAN,
    deleted BOOLEAN,
    maintenance_note VARCHAR(500),
    CONSTRAINT uk_devices_name UNIQUE (name)
);

CREATE TABLE IF NOT EXISTS products (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name VARCHAR(255) NOT NULL,
    price NUMERIC(10, 2) NOT NULL,
    selling_price NUMERIC(10, 2),
    sku VARCHAR(80),
    category VARCHAR(80),
    cost_price NUMERIC(10, 2),
    track_stock BOOLEAN,
    current_stock NUMERIC(14, 3),
    minimum_stock NUMERIC(14, 3),
    unit VARCHAR(30),
    active BOOLEAN NOT NULL,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT uk_products_name UNIQUE (name)
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_products_sku ON products (sku);

CREATE TABLE IF NOT EXISTS app_users (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    username VARCHAR(50) NOT NULL,
    display_name VARCHAR(100) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    role VARCHAR(20) NOT NULL CHECK (role IN ('ADMIN', 'MANAGER', 'CASHIER')),
    rule_id INTEGER,
    active BOOLEAN NOT NULL,
    created_at TIMESTAMP NOT NULL,
    last_login_at TIMESTAMP,
    CONSTRAINT uk_app_users_username UNIQUE (username),
    CONSTRAINT fk_app_users_rule FOREIGN KEY (rule_id) REFERENCES access_rules (id)
);

CREATE TABLE IF NOT EXISTS pricing (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    device_type VARCHAR(255) NOT NULL CHECK (device_type IN ('PS4', 'PS5')),
    session_type VARCHAR(255) NOT NULL CHECK (session_type IN ('SINGLE', 'MULTI', 'MATCH')),
    billing_unit VARCHAR(255) NOT NULL CHECK (billing_unit IN ('HOUR', 'MATCH')),
    price NUMERIC(10, 2) NOT NULL,
    match_duration_minutes INTEGER,
    warning_before_expiry_minutes INTEGER,
    active BOOLEAN NOT NULL,
    CONSTRAINT uk_pricing_device_session UNIQUE (device_type, session_type)
);

CREATE TABLE IF NOT EXISTS game_sessions (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    device_id INTEGER NOT NULL,
    start_time TIMESTAMP NOT NULL,
    end_time TIMESTAMP,
    planned_minutes INTEGER,
    hourly_rate_snapshot NUMERIC(10, 2) NOT NULL,
    session_type VARCHAR(255) CHECK (session_type IN ('SINGLE', 'MULTI', 'MATCH')),
    billing_unit VARCHAR(255) CHECK (billing_unit IN ('HOUR', 'MATCH')),
    unit_price_snapshot NUMERIC(10, 2),
    match_duration_minutes_snapshot INTEGER,
    purchased_matches INTEGER,
    completed_matches INTEGER,
    current_match_started_at TIMESTAMP,
    current_match_expires_at TIMESTAMP,
    warning_before_expiry_minutes_snapshot INTEGER,
    match_expired BOOLEAN,
    final_amount NUMERIC(10, 2),
    status VARCHAR(255) NOT NULL CHECK (status IN ('ACTIVE', 'COMPLETED', 'CANCELLED')),
    CONSTRAINT fk_game_sessions_device FOREIGN KEY (device_id) REFERENCES devices (id)
);

CREATE INDEX IF NOT EXISTS idx_game_sessions_device_status
    ON game_sessions (device_id, status);

CREATE TABLE IF NOT EXISTS cafe_orders (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    game_session_id INTEGER,
    created_at TIMESTAMP NOT NULL,
    completed_at TIMESTAMP,
    status VARCHAR(255) NOT NULL CHECK (status IN ('OPEN', 'HELD', 'COMPLETED', 'CANCELLED')),
    total_amount NUMERIC(10, 2) NOT NULL,
    discount_amount NUMERIC(10, 2),
    discount_reason VARCHAR(200),
    CONSTRAINT fk_cafe_orders_session FOREIGN KEY (game_session_id) REFERENCES game_sessions (id)
);

CREATE INDEX IF NOT EXISTS idx_cafe_orders_session_status
    ON cafe_orders (game_session_id, status);

CREATE TABLE IF NOT EXISTS order_items (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    order_id INTEGER NOT NULL,
    product_id INTEGER NOT NULL,
    quantity INTEGER NOT NULL,
    unit_price_snapshot NUMERIC(10, 2) NOT NULL,
    line_total NUMERIC(10, 2) NOT NULL,
    CONSTRAINT fk_order_items_order FOREIGN KEY (order_id) REFERENCES cafe_orders (id),
    CONSTRAINT fk_order_items_product FOREIGN KEY (product_id) REFERENCES products (id)
);

CREATE TABLE IF NOT EXISTS bills (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    bill_number VARCHAR(255) NOT NULL,
    session_id INTEGER UNIQUE,
    order_id INTEGER UNIQUE,
    gaming_amount NUMERIC(10, 2) NOT NULL,
    order_amount NUMERIC(10, 2) NOT NULL,
    total_amount NUMERIC(10, 2) NOT NULL,
    status VARCHAR(255) NOT NULL CHECK (status IN ('PENDING_PAYMENT', 'PAID', 'REFUNDED', 'CANCELLED')),
    created_at TIMESTAMP NOT NULL,
    paid_at TIMESTAMP,
    refunded_at TIMESTAMP,
    automatic_expiry BOOLEAN,
    notification_expires_at TIMESTAMP,
    refund_reason VARCHAR(500),
    CONSTRAINT uk_bills_bill_number UNIQUE (bill_number),
    CONSTRAINT fk_bills_session FOREIGN KEY (session_id) REFERENCES game_sessions (id),
    CONSTRAINT fk_bills_order FOREIGN KEY (order_id) REFERENCES cafe_orders (id)
);

CREATE INDEX IF NOT EXISTS idx_bills_status ON bills (status);

CREATE TABLE IF NOT EXISTS cashier_shifts (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id INTEGER NOT NULL,
    opened_at TIMESTAMP NOT NULL,
    closed_at TIMESTAMP,
    status VARCHAR(20) NOT NULL CHECK (status IN ('OPEN', 'CLOSED')),
    opening_cash NUMERIC(12, 2) NOT NULL,
    expected_cash NUMERIC(12, 2),
    actual_cash NUMERIC(12, 2),
    cash_difference NUMERIC(12, 2),
    closing_note VARCHAR(500),
    CONSTRAINT fk_cashier_shifts_user FOREIGN KEY (user_id) REFERENCES app_users (id)
);

CREATE TABLE IF NOT EXISTS payments (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    bill_id INTEGER NOT NULL,
    shift_id INTEGER,
    method VARCHAR(255) NOT NULL CHECK (method IN ('CASH', 'CARD', 'MOBILE_WALLET')),
    amount NUMERIC(10, 2) NOT NULL,
    amount_tendered NUMERIC(10, 2) NOT NULL,
    change_amount NUMERIC(10, 2) NOT NULL,
    status VARCHAR(255) NOT NULL CHECK (status IN ('COMPLETED', 'REFUNDED')),
    paid_at TIMESTAMP NOT NULL,
    cashier VARCHAR(80),
    CONSTRAINT fk_payments_bill FOREIGN KEY (bill_id) REFERENCES bills (id),
    CONSTRAINT fk_payments_shift FOREIGN KEY (shift_id) REFERENCES cashier_shifts (id)
);

CREATE TABLE IF NOT EXISTS stock_movements (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    product_id INTEGER NOT NULL,
    type VARCHAR(20) NOT NULL CHECK (type IN ('PURCHASE', 'SALE', 'RETURN', 'ADJUSTMENT', 'WASTE')),
    quantity NUMERIC(14, 3) NOT NULL,
    unit_cost NUMERIC(10, 2),
    reference VARCHAR(160),
    created_at TIMESTAMP NOT NULL,
    created_by VARCHAR(80) NOT NULL,
    CONSTRAINT fk_stock_movements_product FOREIGN KEY (product_id) REFERENCES products (id)
);

CREATE INDEX IF NOT EXISTS idx_stock_movement_product_created
    ON stock_movements (product_id, created_at);
CREATE INDEX IF NOT EXISTS idx_stock_movement_reference
    ON stock_movements (reference);

CREATE TABLE IF NOT EXISTS role_permissions (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    role_name VARCHAR(20) NOT NULL CHECK (role_name IN ('ADMIN', 'MANAGER', 'CASHIER')),
    permission VARCHAR(60) NOT NULL CHECK (permission IN (
        'OPERATIONS_USE', 'RESERVATIONS_MANAGE', 'POS_USE', 'CHECKOUT_USE', 'BILL_REFUND',
        'SHIFT_MANAGE', 'SHIFT_AUDIT', 'DASHBOARD_VIEW', 'PRODUCTS_VIEW',
        'PRODUCTS_MANAGE', 'INVENTORY_VIEW', 'INVENTORY_MANAGE',
        'PRICING_VIEW', 'PRICING_MANAGE', 'REPORTS_VIEW', 'DEVICES_VIEW',
        'DEVICES_MANAGE', 'BILLING_MANAGE', 'DISCOUNTS_MANAGE',
        'USERS_MANAGE', 'PERMISSIONS_MANAGE', 'SETTINGS_MANAGE',
        'DESTRUCTIVE_OPERATIONS'
    )),
    CONSTRAINT uk_role_permission UNIQUE (role_name, permission)
);

CREATE TABLE IF NOT EXISTS rule_permissions (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    rule_id INTEGER NOT NULL,
    permission VARCHAR(60) NOT NULL CHECK (permission IN (
        'OPERATIONS_USE', 'RESERVATIONS_MANAGE', 'POS_USE', 'CHECKOUT_USE', 'BILL_REFUND',
        'SHIFT_MANAGE', 'SHIFT_AUDIT', 'DASHBOARD_VIEW', 'PRODUCTS_VIEW',
        'PRODUCTS_MANAGE', 'INVENTORY_VIEW', 'INVENTORY_MANAGE',
        'PRICING_VIEW', 'PRICING_MANAGE', 'REPORTS_VIEW', 'DEVICES_VIEW',
        'DEVICES_MANAGE', 'BILLING_MANAGE', 'DISCOUNTS_MANAGE',
        'USERS_MANAGE', 'PERMISSIONS_MANAGE', 'SETTINGS_MANAGE',
        'DESTRUCTIVE_OPERATIONS'
    )),
    CONSTRAINT uk_rule_permission UNIQUE (rule_id, permission),
    CONSTRAINT fk_rule_permissions_rule FOREIGN KEY (rule_id) REFERENCES access_rules (id)
);

CREATE TABLE IF NOT EXISTS customers (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name VARCHAR(100) NOT NULL,
    phone VARCHAR(30) NOT NULL,
    email VARCHAR(100),
    notes VARCHAR(500),
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT uk_customers_phone UNIQUE (phone)
);

CREATE TABLE IF NOT EXISTS reservations (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    customer_id INTEGER NOT NULL,
    device_id INTEGER NOT NULL,
    session_type VARCHAR(255) NOT NULL CHECK (session_type IN ('SINGLE', 'MULTI', 'MATCH')),
    start_time TIMESTAMP NOT NULL,
    duration_minutes INTEGER,
    status VARCHAR(20) NOT NULL CHECK (status IN ('UPCOMING', 'CHECKED_IN', 'CANCELLED', 'NO_SHOW')),
    notes VARCHAR(300),
    created_at TIMESTAMP NOT NULL,
    checked_in_at TIMESTAMP,
    cancelled_at TIMESTAMP,
    cancel_reason VARCHAR(200),
    game_session_id INTEGER,
    CONSTRAINT fk_reservations_customer FOREIGN KEY (customer_id) REFERENCES customers (id),
    CONSTRAINT fk_reservations_device FOREIGN KEY (device_id) REFERENCES devices (id),
    CONSTRAINT fk_reservations_game_session FOREIGN KEY (game_session_id) REFERENCES game_sessions (id)
);

CREATE INDEX IF NOT EXISTS idx_reservations_device_status_start
    ON reservations (device_id, status, start_time);

CREATE TABLE IF NOT EXISTS app_settings (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    prevent_negative_stock BOOLEAN NOT NULL,
    dashboard_ending_soon_minutes INTEGER NOT NULL,
    reservations_no_show_grace_minutes INTEGER NOT NULL
);

CREATE TABLE IF NOT EXISTS app_settings_discount_roles (
    app_settings_id INTEGER NOT NULL,
    role VARCHAR(20) NOT NULL CHECK (role IN ('ADMIN', 'MANAGER', 'CASHIER')),
    PRIMARY KEY (app_settings_id, role),
    CONSTRAINT fk_app_settings_discount_roles
        FOREIGN KEY (app_settings_id) REFERENCES app_settings (id)
);
