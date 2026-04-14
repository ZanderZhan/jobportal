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
        TopicExchange eventsExchange = new TopicExchange(BrokerTopology.EVENTS_EXCHANGE, true, false);
        DirectExchange deadLetterExchange = new DirectExchange(BrokerTopology.DEAD_LETTER_EXCHANGE, true, false);

        Queue applicationSubmittedQueue = durableQueue(
                BrokerTopology.APPLICATION_SUBMITTED_QUEUE,
                BrokerTopology.APPLICATION_SUBMITTED_DLQ
        );
        Queue applicationSubmittedRetryQueue = retryQueue(
                BrokerTopology.APPLICATION_SUBMITTED_RETRY_QUEUE,
                BrokerTopology.APPLICATION_SUBMITTED_ROUTING_KEY
        );
        Queue applicationSubmittedDlq = durableQueue(BrokerTopology.APPLICATION_SUBMITTED_DLQ);

        Queue applicationStatusChangedQueue = durableQueue(
                BrokerTopology.APPLICATION_STATUS_CHANGED_QUEUE,
                BrokerTopology.APPLICATION_STATUS_CHANGED_DLQ
        );
        Queue applicationStatusChangedRetryQueue = retryQueue(
                BrokerTopology.APPLICATION_STATUS_CHANGED_RETRY_QUEUE,
                BrokerTopology.APPLICATION_STATUS_CHANGED_ROUTING_KEY
        );
        Queue applicationStatusChangedDlq = durableQueue(BrokerTopology.APPLICATION_STATUS_CHANGED_DLQ);

        Queue applicationWithdrawnQueue = durableQueue(
                BrokerTopology.APPLICATION_WITHDRAWN_QUEUE,
                BrokerTopology.APPLICATION_WITHDRAWN_DLQ
        );
        Queue applicationWithdrawnRetryQueue = retryQueue(
                BrokerTopology.APPLICATION_WITHDRAWN_RETRY_QUEUE,
                BrokerTopology.APPLICATION_WITHDRAWN_ROUTING_KEY
        );
        Queue applicationWithdrawnDlq = durableQueue(BrokerTopology.APPLICATION_WITHDRAWN_DLQ);

        Queue jobPostedQueue = durableQueue(
                BrokerTopology.JOB_POSTED_QUEUE,
                BrokerTopology.JOB_POSTED_DLQ
        );
        Queue jobPostedRetryQueue = retryQueue(
                BrokerTopology.JOB_POSTED_RETRY_QUEUE,
                BrokerTopology.JOB_POSTED_ROUTING_KEY
        );
        Queue jobPostedDlq = durableQueue(BrokerTopology.JOB_POSTED_DLQ);

        Queue employerVerifiedQueue = durableQueue(
                BrokerTopology.EMPLOYER_VERIFIED_QUEUE,
                BrokerTopology.EMPLOYER_VERIFIED_DLQ
        );
        Queue employerVerifiedRetryQueue = retryQueue(
                BrokerTopology.EMPLOYER_VERIFIED_RETRY_QUEUE,
                BrokerTopology.EMPLOYER_VERIFIED_ROUTING_KEY
        );
        Queue employerVerifiedDlq = durableQueue(BrokerTopology.EMPLOYER_VERIFIED_DLQ);

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
                bind(applicationSubmittedQueue, eventsExchange, BrokerTopology.APPLICATION_SUBMITTED_ROUTING_KEY),
                bind(applicationSubmittedRetryQueue, eventsExchange, BrokerTopology.APPLICATION_SUBMITTED_RETRY_ROUTING_KEY),
                bind(applicationSubmittedDlq, deadLetterExchange, BrokerTopology.APPLICATION_SUBMITTED_DLQ),
                bind(applicationStatusChangedQueue, eventsExchange, BrokerTopology.APPLICATION_STATUS_CHANGED_ROUTING_KEY),
                bind(applicationStatusChangedRetryQueue, eventsExchange, BrokerTopology.APPLICATION_STATUS_CHANGED_RETRY_ROUTING_KEY),
                bind(applicationStatusChangedDlq, deadLetterExchange, BrokerTopology.APPLICATION_STATUS_CHANGED_DLQ),
                bind(applicationWithdrawnQueue, eventsExchange, BrokerTopology.APPLICATION_WITHDRAWN_ROUTING_KEY),
                bind(applicationWithdrawnRetryQueue, eventsExchange, BrokerTopology.APPLICATION_WITHDRAWN_RETRY_ROUTING_KEY),
                bind(applicationWithdrawnDlq, deadLetterExchange, BrokerTopology.APPLICATION_WITHDRAWN_DLQ),
                bind(jobPostedQueue, eventsExchange, BrokerTopology.JOB_POSTED_ROUTING_KEY),
                bind(jobPostedRetryQueue, eventsExchange, BrokerTopology.JOB_POSTED_RETRY_ROUTING_KEY),
                bind(jobPostedDlq, deadLetterExchange, BrokerTopology.JOB_POSTED_DLQ),
                bind(employerVerifiedQueue, eventsExchange, BrokerTopology.EMPLOYER_VERIFIED_ROUTING_KEY),
                bind(employerVerifiedRetryQueue, eventsExchange, BrokerTopology.EMPLOYER_VERIFIED_RETRY_ROUTING_KEY),
                bind(employerVerifiedDlq, deadLetterExchange, BrokerTopology.EMPLOYER_VERIFIED_DLQ)
        );
    }

    @Bean
    MessageConverter rabbitMessageConverter() {
        return new JacksonJsonMessageConverter();
    }

    private Queue durableQueue(String name, String deadLetterRoutingKey) {
        return QueueBuilder.durable(name)
                .deadLetterExchange(BrokerTopology.DEAD_LETTER_EXCHANGE)
                .deadLetterRoutingKey(deadLetterRoutingKey)
                .build();
    }

    private Queue retryQueue(String name, String backToRoutingKey) {
        return QueueBuilder.durable(name)
                .ttl((int) BrokerTopology.RETRY_TTL_MILLIS)
                .deadLetterExchange(BrokerTopology.EVENTS_EXCHANGE)
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
