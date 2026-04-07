package com.jobportal.notificationservice.dto;

import com.jobportal.notificationservice.entity.DeliveryChannel;
import com.jobportal.notificationservice.entity.NotificationEventType;

public record NotificationTemplateResponse(
        Long id,
        NotificationEventType eventType,
        DeliveryChannel channel,
        String subjectTemplate,
        String bodyTemplate,
        boolean active
) {
}
