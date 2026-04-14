package com.jobportal.applicationservice.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

@Component
public class ApplicationSchemaRepair {

    private static final Logger log = LoggerFactory.getLogger(ApplicationSchemaRepair.class);

    private final DataSource dataSource;
    private final JdbcTemplate jdbcTemplate;

    public ApplicationSchemaRepair(DataSource dataSource, JdbcTemplate jdbcTemplate) {
        this.dataSource = dataSource;
        this.jdbcTemplate = jdbcTemplate;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void repairLegacySchema() {
        if (!isPostgreSql()) {
            return;
        }

        String nullable = jdbcTemplate.query(
                """
                select is_nullable
                from information_schema.columns
                where table_schema = current_schema()
                  and table_name = 'applications'
                  and column_name = 'employer_id_snapshot'
                """,
                rs -> rs.next() ? rs.getString("is_nullable") : null
        );

        if ("NO".equalsIgnoreCase(nullable)) {
            jdbcTemplate.execute("ALTER TABLE applications ALTER COLUMN employer_id_snapshot DROP NOT NULL");
            log.info("Dropped legacy NOT NULL constraint from applications.employer_id_snapshot");
        }
    }

    private boolean isPostgreSql() {
        try (Connection connection = dataSource.getConnection()) {
            return connection.getMetaData()
                    .getDatabaseProductName()
                    .toLowerCase()
                    .contains("postgresql");
        } catch (SQLException ex) {
            log.warn("Unable to inspect database product while checking application schema", ex);
            return false;
        }
    }
}
