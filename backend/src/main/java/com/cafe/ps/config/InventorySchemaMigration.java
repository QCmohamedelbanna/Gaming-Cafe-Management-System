package com.cafe.ps.config;

import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.boot.CommandLineRunner;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashSet;
import java.util.Set;

/**
 * SQLite's Hibernate schema-update support does not reliably add new columns
 * to an existing table. This migration keeps existing cafe databases usable
 * when the inventory model is introduced. Every statement is idempotent.
 */
@Component
@Order(1)
@RequiredArgsConstructor
public class InventorySchemaMigration implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) {
        if (!tableExists("products")) return;

        Set<String> columns = productColumns();
        addColumn(columns, "selling_price", "NUMERIC(10,2)");
        addColumn(columns, "sku", "VARCHAR(80)");
        addColumn(columns, "category", "VARCHAR(80)");
        addColumn(columns, "cost_price", "NUMERIC(10,2)");
        addColumn(columns, "track_stock", "BOOLEAN");
        addColumn(columns, "current_stock", "NUMERIC(14,3)");
        addColumn(columns, "minimum_stock", "NUMERIC(14,3)");
        addColumn(columns, "unit", "VARCHAR(30)");

        jdbcTemplate.update("""
                UPDATE products
                   SET selling_price = price
                 WHERE selling_price IS NULL
                """);
        jdbcTemplate.update("""
                UPDATE products
                   SET category = 'Uncategorized'
                 WHERE category IS NULL OR trim(category) = ''
                """);
        jdbcTemplate.update("UPDATE products SET cost_price = 0 WHERE cost_price IS NULL");
        jdbcTemplate.update("UPDATE products SET track_stock = 0 WHERE track_stock IS NULL");
        jdbcTemplate.update("UPDATE products SET current_stock = 0 WHERE current_stock IS NULL");
        jdbcTemplate.update("UPDATE products SET minimum_stock = 0 WHERE minimum_stock IS NULL");
        jdbcTemplate.update("UPDATE products SET unit = 'unit' WHERE unit IS NULL OR trim(unit) = ''");

        jdbcTemplate.execute("""
                CREATE UNIQUE INDEX IF NOT EXISTS ux_products_sku
                    ON products(sku)
                """);

        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS stock_movements (
                    id INTEGER PRIMARY KEY,
                    product_id INTEGER NOT NULL,
                    type VARCHAR(20) NOT NULL,
                    quantity NUMERIC(14,3) NOT NULL,
                    unit_cost NUMERIC(10,2),
                    reference VARCHAR(160),
                    created_at TIMESTAMP NOT NULL,
                    created_by VARCHAR(80) NOT NULL,
                    FOREIGN KEY (product_id) REFERENCES products(id)
                )
                """);
        jdbcTemplate.execute("""
                CREATE INDEX IF NOT EXISTS idx_stock_movement_product_created
                    ON stock_movements(product_id, created_at)
                """);
        jdbcTemplate.execute("""
                CREATE INDEX IF NOT EXISTS idx_stock_movement_reference
                    ON stock_movements(reference)
                """);

        migrateOrderColumns();
        migrateOrderStatusConstraint();
        migratePaymentColumns();
    }

    private Set<String> productColumns() {
        return new HashSet<>(jdbcTemplate.query(
                "PRAGMA table_info(products)",
                (resultSet, rowNum) -> resultSet.getString("name")
        ));
    }

    private void addColumn(Set<String> columns, String name, String definition) {
        if (columns.add(name)) {
            jdbcTemplate.execute("ALTER TABLE products ADD COLUMN " + name + " " + definition);
        }
    }

    private boolean tableExists(String tableName) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM sqlite_master WHERE type = 'table' AND name = ?",
                Integer.class,
                tableName
        );
        return count != null && count > 0;
    }

    private void migrateOrderColumns() {
        if (!tableExists("cafe_orders")) return;

        Set<String> columns = new HashSet<>(jdbcTemplate.query(
                "PRAGMA table_info(cafe_orders)",
                (resultSet, rowNum) -> resultSet.getString("name")
        ));
        addOrderColumn(columns, "discount_amount", "NUMERIC(10,2)");
        addOrderColumn(columns, "discount_reason", "VARCHAR(200)");
        jdbcTemplate.update("UPDATE cafe_orders SET discount_amount = 0 WHERE discount_amount IS NULL");
    }

    /**
     * SQLite cannot alter an existing CHECK constraint in place. The original
     * order table was created before HELD was added to OrderStatus, so rebuild
     * it once with the expanded constraint while preserving every row and id.
     */
    private void migrateOrderStatusConstraint() {
        if (!tableExists("cafe_orders")) return;

        String createSql = jdbcTemplate.queryForObject(
                "SELECT sql FROM sqlite_master WHERE type = 'table' AND name = 'cafe_orders'",
                String.class
        );

        if (createSql != null && createSql.toUpperCase().contains("'HELD'")) return;

        jdbcTemplate.execute((org.springframework.jdbc.core.ConnectionCallback<Void>) connection -> {
            boolean previousAutoCommit = connection.getAutoCommit();
            boolean foreignKeysEnabled = foreignKeysEnabled(connection);

            try (Statement statement = connection.createStatement()) {
                statement.execute("PRAGMA foreign_keys = OFF");
                connection.setAutoCommit(false);
                statement.execute("DROP TABLE IF EXISTS cafe_orders_phase4");
                statement.execute("""
                        CREATE TABLE cafe_orders_phase4 (
                            id INTEGER,
                            completed_at TIMESTAMP,
                            created_at TIMESTAMP NOT NULL,
                            status VARCHAR(255) NOT NULL CHECK (status IN ('OPEN','HELD','COMPLETED','CANCELLED')),
                            total_amount NUMERIC(10,2) NOT NULL,
                            game_session_id BIGINT,
                            discount_reason VARCHAR(200),
                            discount_amount NUMERIC(10,2),
                            PRIMARY KEY (id)
                        )
                        """);
                statement.execute("""
                        INSERT INTO cafe_orders_phase4
                            (id, completed_at, created_at, status, total_amount,
                             game_session_id, discount_reason, discount_amount)
                        SELECT id, completed_at, created_at, status, total_amount,
                               game_session_id, discount_reason, discount_amount
                          FROM cafe_orders
                        """);
                statement.execute("DROP TABLE cafe_orders");
                statement.execute("ALTER TABLE cafe_orders_phase4 RENAME TO cafe_orders");
                connection.commit();
                statement.execute("PRAGMA foreign_keys = " + (foreignKeysEnabled ? "ON" : "OFF"));
            } catch (SQLException exception) {
                connection.rollback();
                throw exception;
            } finally {
                if (connection.getAutoCommit() != previousAutoCommit) {
                    connection.setAutoCommit(previousAutoCommit);
                }
            }

            return null;
        });
    }

    private boolean foreignKeysEnabled(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement();
             var resultSet = statement.executeQuery("PRAGMA foreign_keys")) {
            return resultSet.next() && resultSet.getInt(1) != 0;
        }
    }

    private void addOrderColumn(Set<String> columns, String name, String definition) {
        if (columns.add(name)) {
            jdbcTemplate.execute("ALTER TABLE cafe_orders ADD COLUMN " + name + " " + definition);
        }
    }

    private void migratePaymentColumns() {
        if (!tableExists("payments")) return;

        Set<String> columns = new HashSet<>(jdbcTemplate.query(
                "PRAGMA table_info(payments)",
                (resultSet, rowNum) -> resultSet.getString("name")
        ));
        if (columns.add("cashier")) {
            jdbcTemplate.execute("ALTER TABLE payments ADD COLUMN cashier VARCHAR(80)");
        }
        jdbcTemplate.update("UPDATE payments SET cashier = 'Admin' WHERE cashier IS NULL OR trim(cashier) = ''");
    }
}
