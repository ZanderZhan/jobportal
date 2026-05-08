package com.jobportal.notificationservice.dto;

public record NotificationMetricsResponse(
        long totalNotifications,
        long sentNotifications,
        long failedNotifications,
        long retryingNotifications,
        long pendingNotifications,
        long storedPreferences,
        long activeTemplates
) {
}
