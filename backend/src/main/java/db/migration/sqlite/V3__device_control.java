package db.migration.sqlite;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashSet;
import java.util.Set;

/** Adds optional hardware-control metadata without changing existing devices. */
public class V3__device_control extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();
        if (!tableExists(connection, "devices")) return;

        Set<String> columns = columns(connection, "devices");
        addColumn(connection, "devices", columns,
                "control_provider", "VARCHAR(32) NOT NULL DEFAULT 'NONE'");
        addColumn(connection, "devices", columns,
                "controller_device_id", "VARCHAR(255)");
        addColumn(connection, "devices", columns,
                "controller_power_code", "VARCHAR(100)");
        addColumn(connection, "devices", columns,
                "power_control_enabled", "BOOLEAN NOT NULL DEFAULT FALSE");
        addColumn(connection, "devices", columns,
                "physical_power_status", "VARCHAR(32) NOT NULL DEFAULT 'UNKNOWN'");
        addColumn(connection, "devices", columns,
                "last_control_at", "TIMESTAMP");
        addColumn(connection, "devices", columns,
                "last_control_error", "VARCHAR(500)");
        addColumn(connection, "devices", columns,
                "shutdown_policy", "VARCHAR(40) NOT NULL DEFAULT 'NONE'");
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
            while (results.next()) result.add(results.getString("name"));
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
            try (Statement statement = connection.createStatement()) {
                statement.execute("ALTER TABLE " + tableName + " ADD COLUMN " + name + " " + definition);
            }
        }
    }
}
