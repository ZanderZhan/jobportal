package com.jobportal.notificationservice.service;

import com.jobportal.notificationservice.dto.NotificationDispatchEvent;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import static com.jobportal.notificationservice.config.NotificationTopologyProperties.EVENTS_EXCHANGE;
import static com.jobportal.notificationservice.config.NotificationTopologyProperties.NOTIFICATION_DISPATCH_ROUTING_KEY;

@Service
public class RabbitNotificationDispatchPublisher implements NotificationDispatchPublisher {

    private final RabbitTemplate rabbitTemplate;

    public RabbitNotificationDispatchPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @Override
    public void publish(NotificationDispatchEvent event) {
        rabbitTemplate.convertAndSend(EVENTS_EXCHANGE, NOTIFICATION_DISPATCH_ROUTING_KEY, event);
    }
}
