package com.jobportal.notificationservice.dto;

import com.jobportal.notificationservice.entity.NotificationEventType;
import com.jobportal.notificationservice.entity.NotificationStatus;

import java.time.Instant;

public record NotificationResponse(
        Long id,
        String eventKey,
        NotificationEventType eventType,
        Long recipientUserId,
        String recipientEmail,
        String title,
        String body,
        NotificationStatus status,
        boolean read,
        Instant createdAt,
        Instant readAt
) {
}
