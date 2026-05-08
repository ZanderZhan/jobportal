package com.jobportal.notificationservice.dto;

import com.jobportal.notificationservice.entity.NotificationEventType;

import java.time.Instant;

public record NotificationDispatchEvent(
        Long notificationId,
        String recipientId,
        NotificationEventType type,
        Instant createdAt
) {
}
