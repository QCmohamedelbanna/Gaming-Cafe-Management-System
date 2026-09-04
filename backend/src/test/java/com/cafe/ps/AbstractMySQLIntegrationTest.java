package com.cafe.ps;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.UUID;

/**
 * Shared MySQL configuration for integration tests.
 *
 * Tests intentionally require a dedicated local/CI database through
 * TEST_DB_URL, TEST_DB_USER, and TEST_DB_PASSWORD. They never fall back to
 * the application's development database, because the suite mutates data.
 * Flyway applies the production migrations and ddl-auto=validate exercises
 * the same schema-validation path used by the application.
 */
@Tag("mysql")
@ActiveProfiles("test")
public abstract class AbstractMySQLIntegrationTest {

    protected static final String TEST_ADMIN_USERNAME = "integration-admin";
    protected static final String TEST_ADMIN_PASSWORD = UUID.randomUUID().toString();

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * Spring may execute integration-test classes in different orders, while
     * they intentionally share one disposable MySQL schema. Clear mutable
     * business data before each test so subclass cleanup methods do not rely
     * on a particular class order. Security seed data is preserved for auth
     * tests.
    */
    @BeforeEach
    protected void clearSharedBusinessData() {
        jdbcTemplate.update("DELETE FROM app_settings_discount_roles");
        jdbcTemplate.update("DELETE FROM payments");
        jdbcTemplate.update("DELETE FROM bills");
        jdbcTemplate.update("DELETE FROM order_items");
        jdbcTemplate.update("DELETE FROM reservations");
        jdbcTemplate.update("DELETE FROM cafe_orders");
        jdbcTemplate.update("DELETE FROM stock_movements");
        jdbcTemplate.update("DELETE FROM cashier_shifts");
        jdbcTemplate.update("DELETE FROM game_sessions");
        jdbcTemplate.update("DELETE FROM customers");
        jdbcTemplate.update("DELETE FROM app_settings");
        jdbcTemplate.update("DELETE FROM pricing");
        jdbcTemplate.update("DELETE FROM products");
        jdbcTemplate.update("DELETE FROM devices");
    }

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> requiredEnvironmentVariable("TEST_DB_URL"));
        registry.add("spring.datasource.username", () -> requiredEnvironmentVariable("TEST_DB_USER"));
        registry.add("spring.datasource.password", () -> requiredEnvironmentVariable("TEST_DB_PASSWORD"));
        registry.add("spring.datasource.driver-class-name", () -> "com.mysql.cj.jdbc.Driver");
        registry.add("spring.jpa.database-platform", () -> "org.hibernate.dialect.MySQLDialect");
        registry.add("spring.flyway.locations", () -> "classpath:db/migration/mysql");
        registry.add("app.default-admin-username", () -> TEST_ADMIN_USERNAME);
        registry.add("app.default-admin-password", () -> TEST_ADMIN_PASSWORD);
    }

    private static String requiredEnvironmentVariable(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(
                    "Integration tests require environment variable " + name
                            + " pointing to a dedicated MySQL test database."
            );
        }
        return value;
    }
}
