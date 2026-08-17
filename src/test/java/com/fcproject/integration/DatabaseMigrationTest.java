package com.fcproject.integration;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.DriverManager;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Testcontainers(disabledWithoutDocker = true)
class DatabaseMigrationTest {
    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:17-alpine")
            .withDatabaseName("cointrol")
            .withUsername("cointrol")
            .withPassword("cointrol_test_password");

    @Test
    void appliesAllMigrationsToAnEmptyPostgresqlDatabase() throws Exception {
        Flyway flyway = Flyway.configure()
                .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .defaultSchema("access")
                .schemas("access")
                .createSchemas(true)
                .load();

        assertEquals(3, flyway.migrate().migrationsExecuted);

        try (var connection = DriverManager.getConnection(
                POSTGRES.getJdbcUrl(),
                POSTGRES.getUsername(),
                POSTGRES.getPassword()
        ); var statement = connection.createStatement()) {
            try (var tables = statement.executeQuery(
                    "select count(*) from information_schema.tables where table_schema = 'access'"
            )) {
                tables.next();
                assertEquals(5, tables.getInt(1));
            }
            try (var roles = statement.executeQuery("select count(*) from access.roles")) {
                roles.next();
                assertEquals(2, roles.getInt(1));
            }
        }
    }
}
