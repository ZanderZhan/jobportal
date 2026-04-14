package com.jobportal.notificationservice.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

@Component
public class NotificationSchemaRepair {

    private static final Logger log = LoggerFactory.getLogger(NotificationSchemaRepair.class);

    private final DataSource dataSource;
    private final JdbcTemplate jdbcTemplate;

    public NotificationSchemaRepair(DataSource dataSource, JdbcTemplate jdbcTemplate) {
        this.dataSource = dataSource;
        this.jdbcTemplate = jdbcTemplate;
    }

    @EventListener(ApplicationStartedEvent.class)
    public void repairLegacySchema() {
        if (!isPostgreSql()) {
            return;
        }

        String recipientUserIdType = jdbcTemplate.query(
                """
                select data_type
                from information_schema.columns
                where table_schema = current_schema()
                  and table_name = 'notifications'
                  and column_name = 'recipient_user_id'
                """,
                rs -> rs.next() ? rs.getString("data_type") : null
        );

        if (recipientUserIdType != null && !"character varying".equalsIgnoreCase(recipientUserIdType)) {
            jdbcTemplate.execute(
                    """
                    alter table notifications
                    alter column recipient_user_id type varchar(100)
                    using recipient_user_id::text
                    """
            );
            log.info("Aligned notifications.recipient_user_id to varchar for string-based auth ids");
        }

        String actionRequiredType = jdbcTemplate.query(
                """
                select data_type
                from information_schema.columns
                where table_schema = current_schema()
                  and table_name = 'notifications'
                  and column_name = 'action_required'
                """,
                rs -> rs.next() ? rs.getString("data_type") : null
        );

        if (actionRequiredType == null) {
            jdbcTemplate.execute("alter table notifications add column action_required boolean");
            jdbcTemplate.execute("update notifications set action_required = false where action_required is null");
            jdbcTemplate.execute("alter table notifications alter column action_required set default false");
            jdbcTemplate.execute("alter table notifications alter column action_required set not null");
            log.info("Aligned notifications.action_required for old rows");
        }

        // Old enum-style check constraints can block new notification event values after an update.
        dropConstraintIfExists("notification_templates", "notification_templates_event_type_check");
        dropConstraintIfExists("notifications", "notifications_event_type_check");
        dropConstraintIfExists("notifications", "notifications_status_check");
        dropConstraintIfExists("notification_preferences", "notification_preferences_event_type_check");
        dropConstraintIfExists("delivery_records", "delivery_records_status_check");
    }

    private boolean isPostgreSql() {
        try (Connection connection = dataSource.getConnection()) {
            return connection.getMetaData()
                    .getDatabaseProductName()
                    .toLowerCase()
                    .contains("postgresql");
        } catch (SQLException ex) {
            log.warn("Could not inspect the database product while checking notification schema", ex);
            return false;
        }
    }

    private void dropConstraintIfExists(String tableName, String constraintName) {
        jdbcTemplate.execute("alter table " + tableName + " drop constraint if exists " + constraintName);
    }
}
