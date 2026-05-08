package com.jobportal.notificationservice.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "notification.retry")
public record NotificationRetryProperties(
        int batchSize,
        long backoffMinutes,
        int maxEmailAttempts,
        int maxRecipientWarmupRetries,
        long recipientRecoveryMinutes,
        long fixedDelay
) {
}
