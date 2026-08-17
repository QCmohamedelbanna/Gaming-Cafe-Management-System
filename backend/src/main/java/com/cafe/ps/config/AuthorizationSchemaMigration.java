package com.cafe.ps.config;

import com.cafe.ps.entity.Permission;
import com.cafe.ps.entity.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * SQLite cannot update an existing CHECK constraint in place. Rebuild the
 * authorization tables when a new permission is added to the Java enum so an
 * existing database can start without losing its assignments.
 */
@Component
@Order(2)
@RequiredArgsConstructor
public class AuthorizationSchemaMigration implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) {
        migrateRolePermissions();
        migrateRulePermissions();
    }

    private void migrateRolePermissions() {
        if (!tableExists("role_permissions")) return;

        String createSql = tableSql("role_permissions");
        if (containsAllPermissions(createSql)) return;

        rebuildTable(
                "role_permissions",
                """
                        CREATE TABLE role_permissions_phase (
                            id INTEGER PRIMARY KEY,
                            permission VARCHAR(60) NOT NULL CHECK (permission IN (%s)),
                            role_name VARCHAR(20) NOT NULL CHECK (role_name IN (%s))
                        )
                        """.formatted(permissionValues(), roleValues()),
                """
                        INSERT INTO role_permissions_phase (id, permission, role_name)
                        SELECT id, permission, role_name FROM role_permissions
                        """
        );
    }

    private void migrateRulePermissions() {
        if (!tableExists("rule_permissions")) return;

        String createSql = tableSql("rule_permissions");
        if (containsAllPermissions(createSql)) return;

        rebuildTable(
                "rule_permissions",
                """
                        CREATE TABLE rule_permissions_phase (
                            id INTEGER PRIMARY KEY,
                            permission VARCHAR(60) NOT NULL CHECK (permission IN (%s)),
                            rule_id BIGINT NOT NULL
                        )
                        """.formatted(permissionValues()),
                """
                        INSERT INTO rule_permissions_phase (id, permission, rule_id)
                        SELECT id, permission, rule_id FROM rule_permissions
                        """
        );
    }

    private void rebuildTable(String tableName, String createSql, String copySql) {
        jdbcTemplate.execute((org.springframework.jdbc.core.ConnectionCallback<Void>) connection -> {
            boolean previousAutoCommit = connection.getAutoCommit();
            boolean foreignKeysEnabled = foreignKeysEnabled(connection);

            try (Statement statement = connection.createStatement()) {
                statement.execute("PRAGMA foreign_keys = OFF");
                statement.execute("DROP TABLE IF EXISTS " + tableName + "_phase");
                connection.setAutoCommit(false);
                statement.execute(createSql);
                statement.execute(copySql);
                statement.execute("DROP TABLE " + tableName);
                statement.execute("ALTER TABLE " + tableName + "_phase RENAME TO " + tableName);
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

    private boolean tableExists(String tableName) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM sqlite_master WHERE type = 'table' AND name = ?",
                Integer.class,
                tableName
        );
        return count != null && count > 0;
    }

    private String tableSql(String tableName) {
        return jdbcTemplate.queryForObject(
                "SELECT sql FROM sqlite_master WHERE type = 'table' AND name = ?",
                String.class,
                tableName
        );
    }

    private boolean containsAllPermissions(String createSql) {
        if (createSql == null) return false;
        String normalized = createSql.toUpperCase(Locale.ROOT);
        return Arrays.stream(Permission.values())
                .allMatch(permission -> normalized.contains("'" + permission.name() + "'"));
    }

    private static String permissionValues() {
        return Arrays.stream(Permission.values())
                .map(permission -> "'" + permission.name() + "'")
                .collect(Collectors.joining(","));
    }

    private static String roleValues() {
        return Arrays.stream(Role.values())
                .map(role -> "'" + role.name() + "'")
                .collect(Collectors.joining(","));
    }

    private boolean foreignKeysEnabled(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement();
             var resultSet = statement.executeQuery("PRAGMA foreign_keys")) {
            return resultSet.next() && resultSet.getInt(1) != 0;
        }
    }
}
