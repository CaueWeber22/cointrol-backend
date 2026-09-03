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
                .schemas("access", "finance")
                .createSchemas(true)
                .load();

        assertEquals(11, flyway.migrate().migrationsExecuted);

        try (var connection = DriverManager.getConnection(
                POSTGRES.getJdbcUrl(),
                POSTGRES.getUsername(),
                POSTGRES.getPassword()
        ); var statement = connection.createStatement()) {
            try (var tables = statement.executeQuery(
                    "select count(*) from information_schema.tables where table_schema = 'access'"
            )) {
                tables.next();
                assertEquals(7, tables.getInt(1));
            }
            try (var roles = statement.executeQuery("select count(*) from access.roles")) {
                roles.next();
                assertEquals(2, roles.getInt(1));
            }
            try (var tables = statement.executeQuery(
                    "select count(*) from information_schema.tables where table_schema = 'finance'"
            )) {
                tables.next();
                assertEquals(4, tables.getInt(1));
            }
            try (var constraints = statement.executeQuery("""
                    select count(*)
                    from information_schema.table_constraints
                    where constraint_schema = 'finance' and constraint_type = 'FOREIGN KEY'
                    """)) {
                constraints.next();
                assertEquals(7, constraints.getInt(1));
            }
            try (var columns = statement.executeQuery("""
                    select count(*)
                    from information_schema.columns
                    where table_schema = 'finance'
                      and table_name = 'transfer_groups'
                      and column_name in ('status', 'cancel_reason', 'canceled_at', 'version', 'updated_at')
                    """)) {
                columns.next();
                assertEquals(5, columns.getInt(1));
            }
            try (var currencyColumn = statement.executeQuery("""
                    select count(*)
                    from information_schema.columns
                    where table_schema = 'finance'
                      and table_name = 'accounts'
                      and column_name = 'currency'
                      and data_type = 'character varying'
                      and character_maximum_length = 3
                    """)) {
                currencyColumn.next();
                assertEquals(1, currencyColumn.getInt(1));
            }
            try (var securityTables = statement.executeQuery("""
                    select count(*)
                    from information_schema.tables
                    where table_schema = 'access'
                      and table_name in ('login_attempts', 'security_audit_events')
                    """)) {
                securityTables.next();
                assertEquals(2, securityTables.getInt(1));
            }
        }
    }
}
