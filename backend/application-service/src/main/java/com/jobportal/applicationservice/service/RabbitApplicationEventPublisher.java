package com.jobportal.applicationservice.service;

import com.jobportal.applicationservice.event.ApplicationStatusUpdatedEvent;
import com.jobportal.applicationservice.event.ApplicationSubmittedEvent;
import com.jobportal.applicationservice.event.ApplicationWithdrawnEvent;
import com.jobportal.applicationservice.config.CorrelationIdMdcFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessagePostProcessor;
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
    private final String correlationIdHeader;

    public RabbitApplicationEventPublisher(
            RabbitTemplate rabbitTemplate,
            @Value("${messaging.exchange.events:jobportal.domain.events}") String eventsExchange,
            @Value("${messaging.routing-keys.application-submitted:application.submitted}") String applicationSubmittedRoutingKey,
            @Value("${messaging.routing-keys.application-status-changed:application.status-changed}") String applicationStatusChangedRoutingKey,
            @Value("${messaging.routing-keys.application-withdrawn:application.withdrawn}") String applicationWithdrawnRoutingKey,
            @Value("${application.correlation-id-header:X-Request-ID}") String correlationIdHeader) {
        this.rabbitTemplate = rabbitTemplate;
        this.eventsExchange = eventsExchange;
        this.applicationSubmittedRoutingKey = applicationSubmittedRoutingKey;
        this.applicationStatusChangedRoutingKey = applicationStatusChangedRoutingKey;
        this.applicationWithdrawnRoutingKey = applicationWithdrawnRoutingKey;
        this.correlationIdHeader = correlationIdHeader;
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
        String correlationId = MDC.get(CorrelationIdMdcFilter.MDC_KEY);
        try {
            if (hasText(correlationId)) {
                rabbitTemplate.convertAndSend(exchange, routingKey, event, correlationHeaderProcessor(correlationId));
            } else {
                rabbitTemplate.convertAndSend(exchange, routingKey, event);
            }
            log.info(
                    "Published {} event applicationId={} studentId={} jobId={} exchange={} routingKey={} correlationId={}",
                    description,
                    applicationId(event),
                    studentId(event),
                    jobId(event),
                    exchange,
                    routingKey,
                    correlationId
            );
        } catch (AmqpException ex) {
            log.warn(
                    "Failed to publish {} event applicationId={} studentId={} jobId={} exchange={} routingKey={} correlationId={}",
                    description,
                    applicationId(event),
                    studentId(event),
                    jobId(event),
                    exchange,
                    routingKey,
                    correlationId,
                    ex
            );
        }
    }

    private MessagePostProcessor correlationHeaderProcessor(String correlationId) {
        return (Message message) -> {
            message.getMessageProperties().setHeader(correlationIdHeader, correlationId);
            return message;
        };
    }

    private Long applicationId(Object event) {
        if (event instanceof ApplicationSubmittedEvent submittedEvent) {
            return submittedEvent.applicationId();
        }
        if (event instanceof ApplicationStatusUpdatedEvent statusUpdatedEvent) {
            return statusUpdatedEvent.applicationId();
        }
        if (event instanceof ApplicationWithdrawnEvent withdrawnEvent) {
            return withdrawnEvent.applicationId();
        }
        return null;
    }

    private String studentId(Object event) {
        if (event instanceof ApplicationSubmittedEvent submittedEvent) {
            return submittedEvent.studentId();
        }
        if (event instanceof ApplicationStatusUpdatedEvent statusUpdatedEvent) {
            return statusUpdatedEvent.studentId();
        }
        if (event instanceof ApplicationWithdrawnEvent withdrawnEvent) {
            return withdrawnEvent.studentId();
        }
        return null;
    }

    private Long jobId(Object event) {
        if (event instanceof ApplicationSubmittedEvent submittedEvent) {
            return submittedEvent.jobId();
        }
        if (event instanceof ApplicationStatusUpdatedEvent statusUpdatedEvent) {
            return statusUpdatedEvent.jobId();
        }
        if (event instanceof ApplicationWithdrawnEvent withdrawnEvent) {
            return withdrawnEvent.jobId();
        }
        return null;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
