package com.jobportal.notificationservice.dto;

import com.jobportal.notificationservice.entity.DeliveryChannel;
import com.jobportal.notificationservice.entity.NotificationEventType;
import jakarta.validation.constraints.NotNull;

import java.util.Map;

public record TemplatePreviewRequest(
        @NotNull NotificationEventType eventType,
        @NotNull DeliveryChannel channel,
        Map<String, String> templateData
) {
}
