package com.jobportal.notificationservice.service;

import com.jobportal.notificationservice.dto.NotificationDispatchEvent;
import com.jobportal.notificationservice.entity.NotificationEventType;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.time.Instant;

import static com.jobportal.notificationservice.config.NotificationTopologyProperties.EVENTS_EXCHANGE;
import static com.jobportal.notificationservice.config.NotificationTopologyProperties.NOTIFICATION_DISPATCH_ROUTING_KEY;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class RabbitNotificationDispatchPublisherTest {

    @Test
    void shouldPublishDispatchEventToNotificationDispatchRoute() {
        RabbitTemplate rabbitTemplate = mock(RabbitTemplate.class);
        RabbitNotificationDispatchPublisher publisher = new RabbitNotificationDispatchPublisher(rabbitTemplate);
        NotificationDispatchEvent event = new NotificationDispatchEvent(
                101L,
                "student-15",
                NotificationEventType.APPLICATION_SUBMITTED,
                Instant.parse("2026-04-07T22:00:00Z")
        );

        publisher.publish(event);

        verify(rabbitTemplate).convertAndSend(EVENTS_EXCHANGE, NOTIFICATION_DISPATCH_ROUTING_KEY, event);
    }
}
