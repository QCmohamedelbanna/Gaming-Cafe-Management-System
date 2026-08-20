-- Admin-editable business rules, mirroring com.cafe.ps.entity.AppSettings.
-- Always exactly one row, lazily created by SettingsService#get() on first
-- use rather than seeded here, so this migration is schema-only.

CREATE TABLE app_settings (
    id                                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    prevent_negative_stock              BIT(1) NOT NULL,
    dashboard_ending_soon_minutes       INT NOT NULL,
    reservations_no_show_grace_minutes  INT NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE app_settings_discount_roles (
    app_settings_id  BIGINT NOT NULL,
    role             VARCHAR(20) NOT NULL,
    PRIMARY KEY (app_settings_id, role),
    CONSTRAINT fk_app_settings_discount_roles FOREIGN KEY (app_settings_id) REFERENCES app_settings (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
