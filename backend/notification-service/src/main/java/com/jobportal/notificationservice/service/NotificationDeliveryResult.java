package com.jobportal.notificationservice.service;

public record NotificationDeliveryResult(
        boolean waitingForRecipient
) {
}
