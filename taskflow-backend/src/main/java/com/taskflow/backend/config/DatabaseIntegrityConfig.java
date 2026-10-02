package com.taskflow.backend.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Objects;

@Component
public class DatabaseIntegrityConfig implements CommandLineRunner {
    private final JdbcTemplate jdbcTemplate;

    public DatabaseIntegrityConfig(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(String... args) {
        try (Connection connection = Objects.requireNonNull(jdbcTemplate.getDataSource(), "Data source is required")
                .getConnection()) {
            String databaseProduct = connection.getMetaData().getDatabaseProductName();
            if ("H2".equalsIgnoreCase(databaseProduct)) {
                return;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Unable to determine database product", e);
        }

        jdbcTemplate.execute("ALTER TABLE assignments DROP CONSTRAINT IF EXISTS unique_active_user_task");
        jdbcTemplate.execute(
                "CREATE UNIQUE INDEX IF NOT EXISTS idx_unique_active_user_task ON assignments (user_id, task_id) WHERE is_active = TRUE");
    }
}
