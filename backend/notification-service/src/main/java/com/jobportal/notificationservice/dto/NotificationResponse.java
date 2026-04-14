package com.jobportal.notificationservice.dto;

import com.jobportal.notificationservice.entity.NotificationEventType;
import com.jobportal.notificationservice.entity.NotificationStatus;

import java.time.Instant;

public record NotificationResponse(
        Long id,
        String eventKey,
        NotificationEventType eventType,
        String recipientUserId,
        String recipientEmail,
        String recipientName,
        String title,
        String body,
        boolean actionRequired,
        NotificationStatus status,
        boolean read,
        Instant createdAt,
        Instant readAt,
        String lastDeliveryError,
        Instant nextRetryAt
) {
}
