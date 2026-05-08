package com.jobportal.notificationservice.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class NotificationBrokerTopologyConfiguration {

    @Bean
    @ConditionalOnProperty(value = "notification.topology.declare-locally", havingValue = "true")
    Declarables notificationTopology() {
        // This keeps notification-service runnable even before all branches are merged together.
        TopicExchange eventsExchange = new TopicExchange(NotificationTopologyProperties.EVENTS_EXCHANGE, true, false);
        DirectExchange deadLetterExchange = new DirectExchange(NotificationTopologyProperties.DEAD_LETTER_EXCHANGE, true, false);

        Queue applicationSubmittedQueue = durableQueue(
                NotificationTopologyProperties.APPLICATION_SUBMITTED_QUEUE,
                NotificationTopologyProperties.APPLICATION_SUBMITTED_DLQ
        );
        Queue applicationSubmittedRetryQueue = retryQueue(
                NotificationTopologyProperties.APPLICATION_SUBMITTED_RETRY_QUEUE,
                NotificationTopologyProperties.APPLICATION_SUBMITTED_ROUTING_KEY
        );
        Queue applicationSubmittedDlq = durableQueue(NotificationTopologyProperties.APPLICATION_SUBMITTED_DLQ);

        Queue applicationStatusChangedQueue = durableQueue(
                NotificationTopologyProperties.APPLICATION_STATUS_CHANGED_QUEUE,
                NotificationTopologyProperties.APPLICATION_STATUS_CHANGED_DLQ
        );
        Queue applicationStatusChangedRetryQueue = retryQueue(
                NotificationTopologyProperties.APPLICATION_STATUS_CHANGED_RETRY_QUEUE,
                NotificationTopologyProperties.APPLICATION_STATUS_CHANGED_ROUTING_KEY
        );
        Queue applicationStatusChangedDlq = durableQueue(NotificationTopologyProperties.APPLICATION_STATUS_CHANGED_DLQ);

        Queue applicationWithdrawnQueue = durableQueue(
                NotificationTopologyProperties.APPLICATION_WITHDRAWN_QUEUE,
                NotificationTopologyProperties.APPLICATION_WITHDRAWN_DLQ
        );
        Queue applicationWithdrawnRetryQueue = retryQueue(
                NotificationTopologyProperties.APPLICATION_WITHDRAWN_RETRY_QUEUE,
                NotificationTopologyProperties.APPLICATION_WITHDRAWN_ROUTING_KEY
        );
        Queue applicationWithdrawnDlq = durableQueue(NotificationTopologyProperties.APPLICATION_WITHDRAWN_DLQ);

        Queue jobPostedQueue = durableQueue(
                NotificationTopologyProperties.JOB_POSTED_QUEUE,
                NotificationTopologyProperties.JOB_POSTED_DLQ
        );
        Queue jobPostedRetryQueue = retryQueue(
                NotificationTopologyProperties.JOB_POSTED_RETRY_QUEUE,
                NotificationTopologyProperties.JOB_POSTED_ROUTING_KEY
        );
        Queue jobPostedDlq = durableQueue(NotificationTopologyProperties.JOB_POSTED_DLQ);

        Queue employerVerifiedQueue = durableQueue(
                NotificationTopologyProperties.EMPLOYER_VERIFIED_QUEUE,
                NotificationTopologyProperties.EMPLOYER_VERIFIED_DLQ
        );
        Queue employerVerifiedRetryQueue = retryQueue(
                NotificationTopologyProperties.EMPLOYER_VERIFIED_RETRY_QUEUE,
                NotificationTopologyProperties.EMPLOYER_VERIFIED_ROUTING_KEY
        );
        Queue employerVerifiedDlq = durableQueue(NotificationTopologyProperties.EMPLOYER_VERIFIED_DLQ);

        Queue notificationDispatchQueue = durableQueue(
                NotificationTopologyProperties.NOTIFICATION_DISPATCH_QUEUE,
                NotificationTopologyProperties.NOTIFICATION_DISPATCH_DLQ
        );
        Queue notificationDispatchDlq = durableQueue(NotificationTopologyProperties.NOTIFICATION_DISPATCH_DLQ);

        return new Declarables(
                eventsExchange,
                deadLetterExchange,
                applicationSubmittedQueue,
                applicationSubmittedRetryQueue,
                applicationSubmittedDlq,
                applicationStatusChangedQueue,
                applicationStatusChangedRetryQueue,
                applicationStatusChangedDlq,
                applicationWithdrawnQueue,
                applicationWithdrawnRetryQueue,
                applicationWithdrawnDlq,
                jobPostedQueue,
                jobPostedRetryQueue,
                jobPostedDlq,
                employerVerifiedQueue,
                employerVerifiedRetryQueue,
                employerVerifiedDlq,
                notificationDispatchQueue,
                notificationDispatchDlq,
                bind(applicationSubmittedQueue, eventsExchange, NotificationTopologyProperties.APPLICATION_SUBMITTED_ROUTING_KEY),
                bind(applicationSubmittedRetryQueue, eventsExchange, NotificationTopologyProperties.APPLICATION_SUBMITTED_RETRY_ROUTING_KEY),
                bind(applicationSubmittedDlq, deadLetterExchange, NotificationTopologyProperties.APPLICATION_SUBMITTED_DLQ),
                bind(applicationStatusChangedQueue, eventsExchange, NotificationTopologyProperties.APPLICATION_STATUS_CHANGED_ROUTING_KEY),
                bind(applicationStatusChangedRetryQueue, eventsExchange, NotificationTopologyProperties.APPLICATION_STATUS_CHANGED_RETRY_ROUTING_KEY),
                bind(applicationStatusChangedDlq, deadLetterExchange, NotificationTopologyProperties.APPLICATION_STATUS_CHANGED_DLQ),
                bind(applicationWithdrawnQueue, eventsExchange, NotificationTopologyProperties.APPLICATION_WITHDRAWN_ROUTING_KEY),
                bind(applicationWithdrawnRetryQueue, eventsExchange, NotificationTopologyProperties.APPLICATION_WITHDRAWN_RETRY_ROUTING_KEY),
                bind(applicationWithdrawnDlq, deadLetterExchange, NotificationTopologyProperties.APPLICATION_WITHDRAWN_DLQ),
                bind(jobPostedQueue, eventsExchange, NotificationTopologyProperties.JOB_POSTED_ROUTING_KEY),
                bind(jobPostedRetryQueue, eventsExchange, NotificationTopologyProperties.JOB_POSTED_RETRY_ROUTING_KEY),
                bind(jobPostedDlq, deadLetterExchange, NotificationTopologyProperties.JOB_POSTED_DLQ),
                bind(employerVerifiedQueue, eventsExchange, NotificationTopologyProperties.EMPLOYER_VERIFIED_ROUTING_KEY),
                bind(employerVerifiedRetryQueue, eventsExchange, NotificationTopologyProperties.EMPLOYER_VERIFIED_RETRY_ROUTING_KEY),
                bind(employerVerifiedDlq, deadLetterExchange, NotificationTopologyProperties.EMPLOYER_VERIFIED_DLQ),
                bind(notificationDispatchQueue, eventsExchange, NotificationTopologyProperties.NOTIFICATION_DISPATCH_ROUTING_KEY),
                bind(notificationDispatchDlq, deadLetterExchange, NotificationTopologyProperties.NOTIFICATION_DISPATCH_DLQ)
        );
    }

    private Queue durableQueue(String name, String deadLetterRoutingKey) {
        return QueueBuilder.durable(name)
                .deadLetterExchange(NotificationTopologyProperties.DEAD_LETTER_EXCHANGE)
                .deadLetterRoutingKey(deadLetterRoutingKey)
                .build();
    }

    private Queue retryQueue(String name, String backToRoutingKey) {
        return QueueBuilder.durable(name)
                .ttl((int) NotificationTopologyProperties.RETRY_TTL_MILLIS)
                .deadLetterExchange(NotificationTopologyProperties.EVENTS_EXCHANGE)
                .deadLetterRoutingKey(backToRoutingKey)
                .build();
    }

    private Queue durableQueue(String name) {
        return QueueBuilder.durable(name).build();
    }

    private Binding bind(Queue queue, TopicExchange exchange, String routingKey) {
        return BindingBuilder.bind(queue).to(exchange).with(routingKey);
    }

    private Binding bind(Queue queue, DirectExchange exchange, String routingKey) {
        return BindingBuilder.bind(queue).to(exchange).with(routingKey);
    }
}
