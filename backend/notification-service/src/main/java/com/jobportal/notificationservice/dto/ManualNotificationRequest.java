package com.jobportal.notificationservice.dto;

import com.jobportal.notificationservice.entity.NotificationEventType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Map;

public record ManualNotificationRequest(
        @NotNull NotificationEventType eventType,
        @NotBlank String recipientUserId,
        String recipientEmail,
        String recipientName,
        Map<String, String> templateData
) {
}
