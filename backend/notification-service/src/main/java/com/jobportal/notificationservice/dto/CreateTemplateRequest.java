package com.jobportal.notificationservice.dto;

import com.jobportal.notificationservice.entity.DeliveryChannel;
import com.jobportal.notificationservice.entity.NotificationEventType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateTemplateRequest(
        @NotNull NotificationEventType eventType,
        @NotNull DeliveryChannel channel,
        @NotBlank String subjectTemplate,
        @NotBlank String bodyTemplate
) {
}
