package com.jobportal.notificationservice.dto;

import com.jobportal.notificationservice.entity.NotificationEventType;

import java.time.Instant;
import java.util.Map;

public record EventNotificationRequest(
        String eventKey,
        NotificationEventType eventType,
        Long recipientUserId,
        String recipientEmail,
        Instant occurredAt,
        Map<String, String> templateData
) {
}
