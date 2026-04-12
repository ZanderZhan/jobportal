package com.jobportal.messagebroker.config;

public final class BrokerTopology {

    // Main exchange for business events that other services publish.
    public static final String EVENTS_EXCHANGE = "jobportal.domain.events";
    // Dead-letter exchange for messages that fail in downstream processing.
    public static final String DEAD_LETTER_EXCHANGE = "jobportal.domain.events.dlx";

    // Routing keys stay close to the language used in the assignment report.
    public static final String APPLICATION_SUBMITTED_ROUTING_KEY = "application.submitted";
    public static final String APPLICATION_STATUS_CHANGED_ROUTING_KEY = "application.status-changed";
    public static final String APPLICATION_WITHDRAWN_ROUTING_KEY = "application.withdrawn";
    public static final String JOB_POSTED_ROUTING_KEY = "job.posted";
    public static final String EMPLOYER_VERIFIED_ROUTING_KEY = "auth.employer-verified";

    // Each flow gets its own queue so notification handling can stay isolated.
    public static final String APPLICATION_SUBMITTED_QUEUE = "notification.application.submitted";
    public static final String APPLICATION_STATUS_CHANGED_QUEUE = "notification.application.status-changed";
    public static final String APPLICATION_WITHDRAWN_QUEUE = "notification.application.withdrawn";
    public static final String JOB_POSTED_QUEUE = "notification.job.posted";
    public static final String EMPLOYER_VERIFIED_QUEUE = "notification.auth.employer-verified";

    // Dead-letter queues keep failed messages for later inspection or retry.
    public static final String APPLICATION_SUBMITTED_DLQ = "notification.application.submitted.dlq";
    public static final String APPLICATION_STATUS_CHANGED_DLQ = "notification.application.status-changed.dlq";
    public static final String APPLICATION_WITHDRAWN_DLQ = "notification.application.withdrawn.dlq";
    public static final String JOB_POSTED_DLQ = "notification.job.posted.dlq";
    public static final String EMPLOYER_VERIFIED_DLQ = "notification.auth.employer-verified.dlq";

    private BrokerTopology() {
    }
}
