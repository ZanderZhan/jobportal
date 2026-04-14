package com.jobportal.notificationservice.dto;

public record NotificationMetricsResponse(
        long totalNotifications,
        long deliveredNotifications,
        long failedNotifications,
        long retryScheduledNotifications,
        long suppressedNotifications,
        long storedPreferences,
        long activeTemplates
) {
}
