package com.jobportal.notificationservice.config;

public final class NotificationTopologyProperties {

    // These names stay aligned with the message broker branch.
    public static final String EVENTS_EXCHANGE = "jobportal.domain.events";
    public static final String DEAD_LETTER_EXCHANGE = "jobportal.domain.events.dlx";

    public static final String APPLICATION_SUBMITTED_ROUTING_KEY = "application.submitted";
    public static final String APPLICATION_SUBMITTED_RETRY_ROUTING_KEY = "application.submitted.retry";
    public static final String APPLICATION_STATUS_CHANGED_ROUTING_KEY = "application.status-changed";
    public static final String APPLICATION_STATUS_CHANGED_RETRY_ROUTING_KEY = "application.status-changed.retry";
    public static final String APPLICATION_WITHDRAWN_ROUTING_KEY = "application.withdrawn";
    public static final String APPLICATION_WITHDRAWN_RETRY_ROUTING_KEY = "application.withdrawn.retry";
    public static final String JOB_POSTED_ROUTING_KEY = "job.posted";
    public static final String JOB_POSTED_RETRY_ROUTING_KEY = "job.posted.retry";
    public static final String EMPLOYER_VERIFIED_ROUTING_KEY = "auth.employer-verified";
    public static final String EMPLOYER_VERIFIED_RETRY_ROUTING_KEY = "auth.employer-verified.retry";

    // Queue names match the contracts prepared for the broker side.
    public static final String APPLICATION_SUBMITTED_QUEUE = "notification.application.submitted";
    public static final String APPLICATION_SUBMITTED_RETRY_QUEUE = "notification.application.submitted.retry";
    public static final String APPLICATION_STATUS_CHANGED_QUEUE = "notification.application.status-changed";
    public static final String APPLICATION_STATUS_CHANGED_RETRY_QUEUE = "notification.application.status-changed.retry";
    public static final String APPLICATION_WITHDRAWN_QUEUE = "notification.application.withdrawn";
    public static final String APPLICATION_WITHDRAWN_RETRY_QUEUE = "notification.application.withdrawn.retry";
    public static final String JOB_POSTED_QUEUE = "notification.job.posted";
    public static final String JOB_POSTED_RETRY_QUEUE = "notification.job.posted.retry";
    public static final String EMPLOYER_VERIFIED_QUEUE = "notification.auth.employer-verified";
    public static final String EMPLOYER_VERIFIED_RETRY_QUEUE = "notification.auth.employer-verified.retry";

    public static final String APPLICATION_SUBMITTED_DLQ = "notification.application.submitted.dlq";
    public static final String APPLICATION_STATUS_CHANGED_DLQ = "notification.application.status-changed.dlq";
    public static final String APPLICATION_WITHDRAWN_DLQ = "notification.application.withdrawn.dlq";
    public static final String JOB_POSTED_DLQ = "notification.job.posted.dlq";
    public static final String EMPLOYER_VERIFIED_DLQ = "notification.auth.employer-verified.dlq";

    public static final long RETRY_TTL_MILLIS = 10_000L;

    private NotificationTopologyProperties() {
    }
}
