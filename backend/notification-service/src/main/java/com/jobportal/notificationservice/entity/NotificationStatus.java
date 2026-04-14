package com.jobportal.notificationservice.entity;

public enum NotificationStatus {
    CREATED,
    PENDING_RECIPIENT,
    RETRY_SCHEDULED,
    PARTIALLY_DELIVERED,
    DELIVERED,
    FAILED,
    SUPPRESSED
}
