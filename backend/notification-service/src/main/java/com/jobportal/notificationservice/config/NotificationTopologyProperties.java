package com.jobportal.notificationservice.config;

public final class NotificationTopologyProperties {

    // These names stay aligned with the message broker branch.
    public static final String EVENTS_EXCHANGE = "jobportal.domain.events";
    public static final String DEAD_LETTER_EXCHANGE = "jobportal.domain.events.dlx";

    public static final String APPLICATION_SUBMITTED_ROUTING_KEY = "application.submitted";
    public static final String APPLICATION_STATUS_CHANGED_ROUTING_KEY = "application.status-changed";
    public static final String JOB_POSTED_ROUTING_KEY = "job.posted";
    public static final String EMPLOYER_VERIFIED_ROUTING_KEY = "auth.employer-verified";

    // Queue names match the contracts prepared for the broker side.
    public static final String APPLICATION_SUBMITTED_QUEUE = "notification.application.submitted";
    public static final String APPLICATION_STATUS_CHANGED_QUEUE = "notification.application.status-changed";
    public static final String JOB_POSTED_QUEUE = "notification.job.posted";
    public static final String EMPLOYER_VERIFIED_QUEUE = "notification.auth.employer-verified";

    public static final String APPLICATION_SUBMITTED_DLQ = "notification.application.submitted.dlq";
    public static final String APPLICATION_STATUS_CHANGED_DLQ = "notification.application.status-changed.dlq";
    public static final String JOB_POSTED_DLQ = "notification.job.posted.dlq";
    public static final String EMPLOYER_VERIFIED_DLQ = "notification.auth.employer-verified.dlq";

    private NotificationTopologyProperties() {
    }
}
