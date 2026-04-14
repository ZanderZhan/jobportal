package com.jobportal.notificationservice.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "notification.mail")
public record NotificationMailProperties(
        String fromAddress,
        String fromName
) {
}
