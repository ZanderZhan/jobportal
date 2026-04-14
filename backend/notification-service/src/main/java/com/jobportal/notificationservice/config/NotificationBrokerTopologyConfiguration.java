package com.jobportal.notificationservice.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class NotificationBrokerTopologyConfiguration {

    @Bean
    Declarables notificationTopology() {
        // This lets notification-service run locally even before the broker branch is merged.
        TopicExchange eventsExchange = new TopicExchange(NotificationTopologyProperties.EVENTS_EXCHANGE, true, false);
        DirectExchange deadLetterExchange = new DirectExchange(NotificationTopologyProperties.DEAD_LETTER_EXCHANGE, true, false);

        Queue applicationSubmittedQueue = durableQueue(
                NotificationTopologyProperties.APPLICATION_SUBMITTED_QUEUE,
                NotificationTopologyProperties.APPLICATION_SUBMITTED_DLQ
        );
        Queue applicationSubmittedDlq = durableQueue(NotificationTopologyProperties.APPLICATION_SUBMITTED_DLQ);

        Queue applicationStatusChangedQueue = durableQueue(
                NotificationTopologyProperties.APPLICATION_STATUS_CHANGED_QUEUE,
                NotificationTopologyProperties.APPLICATION_STATUS_CHANGED_DLQ
        );
        Queue applicationStatusChangedDlq = durableQueue(NotificationTopologyProperties.APPLICATION_STATUS_CHANGED_DLQ);

        Queue jobPostedQueue = durableQueue(
                NotificationTopologyProperties.JOB_POSTED_QUEUE,
                NotificationTopologyProperties.JOB_POSTED_DLQ
        );
        Queue jobPostedDlq = durableQueue(NotificationTopologyProperties.JOB_POSTED_DLQ);

        Queue employerVerifiedQueue = durableQueue(
                NotificationTopologyProperties.EMPLOYER_VERIFIED_QUEUE,
                NotificationTopologyProperties.EMPLOYER_VERIFIED_DLQ
        );
        Queue employerVerifiedDlq = durableQueue(NotificationTopologyProperties.EMPLOYER_VERIFIED_DLQ);

        return new Declarables(
                eventsExchange,
                deadLetterExchange,
                applicationSubmittedQueue,
                applicationSubmittedDlq,
                applicationStatusChangedQueue,
                applicationStatusChangedDlq,
                jobPostedQueue,
                jobPostedDlq,
                employerVerifiedQueue,
                employerVerifiedDlq,
                bind(applicationSubmittedQueue, eventsExchange, NotificationTopologyProperties.APPLICATION_SUBMITTED_ROUTING_KEY),
                bind(applicationSubmittedDlq, deadLetterExchange, NotificationTopologyProperties.APPLICATION_SUBMITTED_DLQ),
                bind(applicationStatusChangedQueue, eventsExchange, NotificationTopologyProperties.APPLICATION_STATUS_CHANGED_ROUTING_KEY),
                bind(applicationStatusChangedDlq, deadLetterExchange, NotificationTopologyProperties.APPLICATION_STATUS_CHANGED_DLQ),
                bind(jobPostedQueue, eventsExchange, NotificationTopologyProperties.JOB_POSTED_ROUTING_KEY),
                bind(jobPostedDlq, deadLetterExchange, NotificationTopologyProperties.JOB_POSTED_DLQ),
                bind(employerVerifiedQueue, eventsExchange, NotificationTopologyProperties.EMPLOYER_VERIFIED_ROUTING_KEY),
                bind(employerVerifiedDlq, deadLetterExchange, NotificationTopologyProperties.EMPLOYER_VERIFIED_DLQ)
        );
    }

    private Queue durableQueue(String name, String deadLetterRoutingKey) {
        // Failed messages are kept in a DLQ so they can be retried later.
        return QueueBuilder.durable(name)
                .deadLetterExchange(NotificationTopologyProperties.DEAD_LETTER_EXCHANGE)
                .deadLetterRoutingKey(deadLetterRoutingKey)
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
