package com.jobportal.notificationservice.dto;

import com.jobportal.notificationservice.entity.DeliveryChannel;
import com.jobportal.notificationservice.entity.NotificationEventType;

public record TemplatePreviewResponse(
        NotificationEventType eventType,
        DeliveryChannel channel,
        String subject,
        String body
) {
}
