package com.jobportal.messagebroker.contract;

import java.time.Instant;

public record NotificationDispatchEvent(
        Long notificationId,
        String recipientId,
        String type,
        Instant createdAt
) {
}
