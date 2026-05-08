package com.jobportal.notificationservice.exception;

public class NotificationRetryNotAllowedException extends RuntimeException {

    public NotificationRetryNotAllowedException(Long notificationId, String reason) {
        super("Notification " + notificationId + " cannot be retried: " + reason);
    }
}
