package com.jobportal.notificationservice.dto;

import jakarta.validation.constraints.NotNull;

public record UpdateNotificationPreferenceRequest(
        @NotNull Boolean inAppEnabled,
        @NotNull Boolean emailEnabled
) {
}
