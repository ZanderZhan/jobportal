package com.jobportal.notificationservice.dto;

import com.jobportal.notificationservice.entity.NotificationEventType;

import java.time.Instant;
import java.util.Map;

public record EventNotificationRequest(
        String eventKey,
        NotificationEventType eventType,
        String recipientUserId,
        String recipientEmail,
        String recipientName,
        Instant occurredAt,
        Map<String, String> templateData
) {
}
