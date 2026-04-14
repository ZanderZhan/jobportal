package com.jobportal.notificationservice.dto;

import java.time.Instant;

public record NotificationSummaryResponse(
        long totalCount,
        long unreadCount,
        long actionRequiredCount,
        long failedCount,
        long pendingRecipientCount,
        long retryScheduledCount,
        Instant latestNotificationAt
) {
}
