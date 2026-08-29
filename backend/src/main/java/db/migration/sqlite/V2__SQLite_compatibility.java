package db.migration.sqlite;

import com.cafe.ps.entity.Permission;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Non-destructive compatibility migration for SQLite files created by the
 * pre-Flyway application. It adds missing columns and rebuilds only the old
 * permission CHECK constraints that predate RESERVATIONS_MANAGE.
 */
public class V2__SQLite_compatibility extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();
        ensureLegacyColumns(connection);
        repairPermissionConstraints(connection, "role_permissions", false);
        repairPermissionConstraints(connection, "rule_permissions", true);
    }

    private void ensureLegacyColumns(Connection connection) throws SQLException {
        if (tableExists(connection, "products")) {
            Set<String> columns = columns(connection, "products");
            addColumn(connection, "products", columns, "selling_price", "NUMERIC(10,2)");
            addColumn(connection, "products", columns, "sku", "VARCHAR(80)");
            addColumn(connection, "products", columns, "category", "VARCHAR(80)");
            addColumn(connection, "products", columns, "cost_price", "NUMERIC(10,2)");
            addColumn(connection, "products", columns, "track_stock", "BOOLEAN");
            addColumn(connection, "products", columns, "current_stock", "NUMERIC(14,3)");
            addColumn(connection, "products", columns, "minimum_stock", "NUMERIC(14,3)");
            addColumn(connection, "products", columns, "unit", "VARCHAR(30)");

            execute(connection, "UPDATE products SET selling_price = price WHERE selling_price IS NULL");
            execute(connection, "UPDATE products SET category = 'Uncategorized' WHERE category IS NULL OR trim(category) = ''");
            execute(connection, "UPDATE products SET cost_price = 0 WHERE cost_price IS NULL");
            execute(connection, "UPDATE products SET track_stock = 0 WHERE track_stock IS NULL");
            execute(connection, "UPDATE products SET current_stock = 0 WHERE current_stock IS NULL");
            execute(connection, "UPDATE products SET minimum_stock = 0 WHERE minimum_stock IS NULL");
            execute(connection, "UPDATE products SET unit = 'unit' WHERE unit IS NULL OR trim(unit) = ''");
            execute(connection, "CREATE UNIQUE INDEX IF NOT EXISTS ux_products_sku ON products(sku)");
        }

        if (tableExists(connection, "cafe_orders")) {
            Set<String> columns = columns(connection, "cafe_orders");
            addColumn(connection, "cafe_orders", columns, "discount_amount", "NUMERIC(10,2)");
            addColumn(connection, "cafe_orders", columns, "discount_reason", "VARCHAR(200)");
            execute(connection, "UPDATE cafe_orders SET discount_amount = 0 WHERE discount_amount IS NULL");
        }

        if (tableExists(connection, "payments")) {
            Set<String> columns = columns(connection, "payments");
            addColumn(connection, "payments", columns, "cashier", "VARCHAR(80)");
            execute(connection, "UPDATE payments SET cashier = 'Admin' WHERE cashier IS NULL OR trim(cashier) = ''");
        }
    }

    private void repairPermissionConstraints(
            Connection connection,
            String tableName,
            boolean includesRuleId
    ) throws SQLException {
        if (!tableExists(connection, tableName)) return;

        String createSql = tableSql(connection, tableName);
        if (containsEveryPermission(createSql)) return;

        boolean previousForeignKeys = foreignKeysEnabled(connection);
        try {
            execute(connection, "PRAGMA foreign_keys = OFF");
            String phaseTable = tableName + "_compatibility";
            execute(connection, "DROP TABLE IF EXISTS " + phaseTable);
            if (includesRuleId) {
                execute(connection, """
                        CREATE TABLE %s (
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
                            CONSTRAINT fk_rule_permissions_rule
                                FOREIGN KEY (rule_id) REFERENCES access_rules(id)
                        )
                        """.formatted(phaseTable));
                execute(connection, """
                        INSERT INTO %s (id, rule_id, permission)
                        SELECT id, rule_id, permission FROM %s
                        """.formatted(phaseTable, tableName));
            } else {
                execute(connection, """
                        CREATE TABLE %s (
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
                        )
                        """.formatted(phaseTable));
                execute(connection, """
                        INSERT INTO %s (id, role_name, permission)
                        SELECT id, role_name, permission FROM %s
                        """.formatted(phaseTable, tableName));
            }
            execute(connection, "DROP TABLE " + tableName);
            execute(connection, "ALTER TABLE " + phaseTable + " RENAME TO " + tableName);
        } finally {
            execute(connection, "PRAGMA foreign_keys = " + (previousForeignKeys ? "ON" : "OFF"));
        }
    }

    private static boolean containsEveryPermission(String createSql) {
        if (createSql == null) return false;
        String normalized = createSql.toUpperCase(Locale.ROOT);
        return Arrays.stream(Permission.values())
                .allMatch(permission -> normalized.contains("'" + permission.name() + "'"));
    }

    private static String tableSql(Connection connection, String tableName) throws SQLException {
        try (var statement = connection.prepareStatement(
                "SELECT sql FROM sqlite_master WHERE type = 'table' AND name = ?"
        )) {
            statement.setString(1, tableName);
            try (ResultSet results = statement.executeQuery()) {
                return results.next() ? results.getString(1) : null;
            }
        }
    }

    private static boolean tableExists(Connection connection, String tableName) throws SQLException {
        try (var statement = connection.prepareStatement(
                "SELECT count(*) FROM sqlite_master WHERE type = 'table' AND name = ?"
        )) {
            statement.setString(1, tableName);
            try (ResultSet results = statement.executeQuery()) {
                return results.next() && results.getInt(1) > 0;
            }
        }
    }

    private static Set<String> columns(Connection connection, String tableName) throws SQLException {
        Set<String> result = new HashSet<>();
        try (Statement statement = connection.createStatement();
             ResultSet results = statement.executeQuery("PRAGMA table_info(" + tableName + ")")) {
            while (results.next()) {
                result.add(results.getString("name"));
            }
        }
        return result;
    }

    private static void addColumn(
            Connection connection,
            String tableName,
            Set<String> columns,
            String name,
            String definition
    ) throws SQLException {
        if (columns.add(name)) {
            execute(connection, "ALTER TABLE " + tableName + " ADD COLUMN " + name + " " + definition);
        }
    }

    private static boolean foreignKeysEnabled(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement();
             ResultSet results = statement.executeQuery("PRAGMA foreign_keys")) {
            return results.next() && results.getInt(1) != 0;
        }
    }

    private static void execute(Connection connection, String sql) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }
}
