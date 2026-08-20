package com.cafe.ps;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;

/**
 * Shared MySQL 8.4 container for integration tests, matching the database
 * used in production (see docker-compose.yml). Started once as a singleton
 * and left running for the JVM's lifetime (Testcontainers' Ryuk sidecar
 * removes it afterwards) so every test class shares one container instead
 * of paying its ~10s startup cost per class. Flyway applies the same
 * db/migration scripts used in production against it, and ddl-auto=validate
 * (set on each subclass) exercises the exact schema-validation path prod
 * runs under. Requires a Docker daemon reachable from the test JVM.
 */
public abstract class AbstractMySQLIntegrationTest {

    protected static final MySQLContainer<?> MYSQL =
            new MySQLContainer<>("mysql:8.4")
                    .withDatabaseName("ps_cafe_test")
                    .withUsername("ps_user")
                    .withPassword("ps_password");

    static {
        MYSQL.start();
    }

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
        registry.add("spring.datasource.driver-class-name", MYSQL::getDriverClassName);
    }
}
