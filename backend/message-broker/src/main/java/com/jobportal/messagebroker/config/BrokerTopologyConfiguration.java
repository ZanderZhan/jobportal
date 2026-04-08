package com.jobportal.messagebroker.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BrokerTopologyConfiguration {

    @Bean
    Declarables notificationTopology() {
        // One shared topic exchange keeps publishers simple and consumers flexible.
        TopicExchange eventsExchange = new TopicExchange(BrokerTopology.EVENTS_EXCHANGE, true, false);
        DirectExchange deadLetterExchange = new DirectExchange(BrokerTopology.DEAD_LETTER_EXCHANGE, true, false);

        // Notification service can treat each event family as a separate workload.
        Queue applicationSubmittedQueue = durableQueue(
                BrokerTopology.APPLICATION_SUBMITTED_QUEUE,
                BrokerTopology.APPLICATION_SUBMITTED_DLQ
        );
        Queue applicationSubmittedDlq = durableQueue(BrokerTopology.APPLICATION_SUBMITTED_DLQ);

        Queue applicationStatusChangedQueue = durableQueue(
                BrokerTopology.APPLICATION_STATUS_CHANGED_QUEUE,
                BrokerTopology.APPLICATION_STATUS_CHANGED_DLQ
        );
        Queue applicationStatusChangedDlq = durableQueue(BrokerTopology.APPLICATION_STATUS_CHANGED_DLQ);

        Queue jobPostedQueue = durableQueue(
                BrokerTopology.JOB_POSTED_QUEUE,
                BrokerTopology.JOB_POSTED_DLQ
        );
        Queue jobPostedDlq = durableQueue(BrokerTopology.JOB_POSTED_DLQ);

        Queue employerVerifiedQueue = durableQueue(
                BrokerTopology.EMPLOYER_VERIFIED_QUEUE,
                BrokerTopology.EMPLOYER_VERIFIED_DLQ
        );
        Queue employerVerifiedDlq = durableQueue(BrokerTopology.EMPLOYER_VERIFIED_DLQ);

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
                bind(applicationSubmittedQueue, eventsExchange, BrokerTopology.APPLICATION_SUBMITTED_ROUTING_KEY),
                bind(applicationSubmittedDlq, deadLetterExchange, BrokerTopology.APPLICATION_SUBMITTED_DLQ),
                bind(applicationStatusChangedQueue, eventsExchange, BrokerTopology.APPLICATION_STATUS_CHANGED_ROUTING_KEY),
                bind(applicationStatusChangedDlq, deadLetterExchange, BrokerTopology.APPLICATION_STATUS_CHANGED_DLQ),
                bind(jobPostedQueue, eventsExchange, BrokerTopology.JOB_POSTED_ROUTING_KEY),
                bind(jobPostedDlq, deadLetterExchange, BrokerTopology.JOB_POSTED_DLQ),
                bind(employerVerifiedQueue, eventsExchange, BrokerTopology.EMPLOYER_VERIFIED_ROUTING_KEY),
                bind(employerVerifiedDlq, deadLetterExchange, BrokerTopology.EMPLOYER_VERIFIED_DLQ)
        );
    }

    @Bean
    MessageConverter rabbitMessageConverter() {
        // JSON payloads make the event contract easy to read and share across services.
        return new JacksonJsonMessageConverter();
    }

    private Queue durableQueue(String name, String deadLetterRoutingKey) {
        // Failed messages are pushed to DLQ instead of being lost silently.
        return QueueBuilder.durable(name)
                .deadLetterExchange(BrokerTopology.DEAD_LETTER_EXCHANGE)
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
