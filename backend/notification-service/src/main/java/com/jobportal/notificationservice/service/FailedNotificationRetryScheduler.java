package com.jobportal.notificationservice.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class FailedNotificationRetryScheduler {

    private final NotificationWorkflowService notificationWorkflowService;

    public FailedNotificationRetryScheduler(NotificationWorkflowService notificationWorkflowService) {
        this.notificationWorkflowService = notificationWorkflowService;
    }

    @Scheduled(fixedDelayString = "${notification.retry.fixed-delay:300000}")
    public void retryDueEmails() {
        // This keeps failed email work moving even if nobody clicks retry manually.
        notificationWorkflowService.retryDueNotifications();
    }
}
