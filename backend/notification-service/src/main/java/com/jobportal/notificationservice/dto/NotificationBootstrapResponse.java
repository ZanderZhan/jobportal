package com.jobportal.notificationservice.dto;

public record NotificationBootstrapResponse(
        String recipientUserId,
        String recipientEmail,
        String recipientName,
        boolean emailReady,
        NotificationSummaryResponse summary
) {
}
