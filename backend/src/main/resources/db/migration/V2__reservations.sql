-- Reservations and customers, mirroring com.cafe.ps.entity.Customer and
-- com.cafe.ps.entity.Reservation.

CREATE TABLE customers (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(100) NOT NULL,
    phone       VARCHAR(30)  NOT NULL,
    email       VARCHAR(100),
    notes       VARCHAR(500),
    created_at  DATETIME(6)  NOT NULL,
    CONSTRAINT uk_customers_phone UNIQUE (phone)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE reservations (
    id                 BIGINT AUTO_INCREMENT PRIMARY KEY,
    customer_id        BIGINT NOT NULL,
    device_id          BIGINT NOT NULL,
    session_type       VARCHAR(255) NOT NULL,
    start_time         DATETIME(6) NOT NULL,
    duration_minutes   INT NOT NULL,
    status             VARCHAR(20) NOT NULL,
    notes              VARCHAR(300),
    created_at         DATETIME(6) NOT NULL,
    checked_in_at      DATETIME(6),
    cancelled_at       DATETIME(6),
    cancel_reason      VARCHAR(200),
    game_session_id    BIGINT,
    CONSTRAINT fk_reservations_customer     FOREIGN KEY (customer_id)     REFERENCES customers (id),
    CONSTRAINT fk_reservations_device       FOREIGN KEY (device_id)       REFERENCES devices (id),
    CONSTRAINT fk_reservations_game_session FOREIGN KEY (game_session_id) REFERENCES game_sessions (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_reservations_device_status_start ON reservations (device_id, status, start_time);
