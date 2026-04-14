package com.jobportal.notificationservice.dto;

import com.jobportal.notificationservice.entity.NotificationEventType;

public record NotificationPreferenceResponse(
        NotificationEventType eventType,
        boolean inAppEnabled,
        boolean emailEnabled
) {
}
