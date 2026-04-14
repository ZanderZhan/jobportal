package com.jobportal.notificationservice.service;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import static com.jobportal.notificationservice.config.NotificationTopologyProperties.EVENTS_EXCHANGE;

@Service
public class NotificationRetryPublisher {

    public static final String RETRY_COUNT_HEADER = "x-notification-retry-count";

    private final RabbitTemplate rabbitTemplate;

    public NotificationRetryPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publishDelayedRetry(String retryRoutingKey, Object payload, int retryCount) {
        rabbitTemplate.convertAndSend(EVENTS_EXCHANGE, retryRoutingKey, payload, message -> {
            message.getMessageProperties().setHeader(RETRY_COUNT_HEADER, retryCount);
            return message;
        });
    }
}
