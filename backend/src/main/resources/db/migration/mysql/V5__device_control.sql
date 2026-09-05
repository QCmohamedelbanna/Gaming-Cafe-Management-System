-- Optional hardware-control metadata. Existing devices remain software-only.
ALTER TABLE devices
    ADD COLUMN control_provider VARCHAR(32) NOT NULL DEFAULT 'NONE',
    ADD COLUMN controller_device_id VARCHAR(255),
    ADD COLUMN controller_power_code VARCHAR(100),
    ADD COLUMN power_control_enabled BIT(1) NOT NULL DEFAULT b'0',
    ADD COLUMN physical_power_status VARCHAR(32) NOT NULL DEFAULT 'UNKNOWN',
    ADD COLUMN last_control_at DATETIME(6),
    ADD COLUMN last_control_error VARCHAR(500),
    ADD COLUMN shutdown_policy VARCHAR(40) NOT NULL DEFAULT 'NONE';
