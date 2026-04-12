package com.jobportal.applicationservice.service;

import com.jobportal.applicationservice.event.ApplicationStatusUpdatedEvent;
import com.jobportal.applicationservice.event.ApplicationSubmittedEvent;
import com.jobportal.applicationservice.event.ApplicationWithdrawnEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class RabbitApplicationEventPublisher implements ApplicationEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(RabbitApplicationEventPublisher.class);

    private final RabbitTemplate rabbitTemplate;
    private final String eventsExchange;
    private final String applicationSubmittedRoutingKey;
    private final String applicationStatusChangedRoutingKey;
    private final String applicationWithdrawnRoutingKey;

    public RabbitApplicationEventPublisher(
            RabbitTemplate rabbitTemplate,
            @Value("${messaging.exchange.events:jobportal.domain.events}") String eventsExchange,
            @Value("${messaging.routing-keys.application-submitted:application.submitted}") String applicationSubmittedRoutingKey,
            @Value("${messaging.routing-keys.application-status-changed:application.status-changed}") String applicationStatusChangedRoutingKey,
            @Value("${messaging.routing-keys.application-withdrawn:application.withdrawn}") String applicationWithdrawnRoutingKey) {
        this.rabbitTemplate = rabbitTemplate;
        this.eventsExchange = eventsExchange;
        this.applicationSubmittedRoutingKey = applicationSubmittedRoutingKey;
        this.applicationStatusChangedRoutingKey = applicationStatusChangedRoutingKey;
        this.applicationWithdrawnRoutingKey = applicationWithdrawnRoutingKey;
    }

    @Override
    public void publishSubmitted(ApplicationSubmittedEvent event) {
        publish(eventsExchange, applicationSubmittedRoutingKey, event, "application submitted");
    }

    @Override
    public void publishStatusUpdated(ApplicationStatusUpdatedEvent event) {
        publish(eventsExchange, applicationStatusChangedRoutingKey, event, "application status changed");
    }

    @Override
    public void publishWithdrawn(ApplicationWithdrawnEvent event) {
        publish(eventsExchange, applicationWithdrawnRoutingKey, event, "application withdrawn");
    }

    private void publish(String exchange, String routingKey, Object event, String description) {
        try {
            rabbitTemplate.convertAndSend(exchange, routingKey, event);
        } catch (AmqpException ex) {
            log.warn("Failed to publish {} event with routing key {}", description, routingKey, ex);
        }
    }
}
