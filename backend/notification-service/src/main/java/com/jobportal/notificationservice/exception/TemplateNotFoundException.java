package com.jobportal.notificationservice.exception;

import com.jobportal.notificationservice.entity.DeliveryChannel;
import com.jobportal.notificationservice.entity.NotificationEventType;

public class TemplateNotFoundException extends RuntimeException {

    public TemplateNotFoundException(NotificationEventType eventType, DeliveryChannel channel) {
        super("Template not found for eventType=" + eventType + ", channel=" + channel);
    }
}
