package com.jobportal.notificationservice.service;

import com.jobportal.notificationservice.dto.NotificationResponse;

public record NotificationProcessingResult(
        Long notificationId,
        NotificationResponse notification,
        boolean waitingForRecipient
) {
}
