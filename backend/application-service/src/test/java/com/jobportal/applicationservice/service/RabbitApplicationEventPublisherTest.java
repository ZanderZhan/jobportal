package com.jobportal.applicationservice.service;

import com.jobportal.applicationservice.event.ApplicationStatusUpdatedEvent;
import com.jobportal.applicationservice.event.ApplicationSubmittedEvent;
import com.jobportal.applicationservice.event.ApplicationWithdrawnEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.time.Instant;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RabbitApplicationEventPublisherTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    private RabbitApplicationEventPublisher publisher;

    @BeforeEach
    void setUp() {
        publisher = new RabbitApplicationEventPublisher(
                rabbitTemplate,
                "jobportal.domain.events",
                "application.submitted",
                "application.status-changed",
                "application.withdrawn"
        );
    }

    @Test
    void publishSubmitted_ShouldUseSubmittedRoutingKey() {
        ApplicationSubmittedEvent event = new ApplicationSubmittedEvent(
                11L,
                "student-1",
                91L,
                Instant.parse("2026-04-12T18:00:00Z")
        );

        publisher.publishSubmitted(event);

        verify(rabbitTemplate).convertAndSend("jobportal.domain.events", "application.submitted", event);
    }

    @Test
    void publishStatusUpdated_ShouldUseStatusChangedRoutingKey() {
        ApplicationStatusUpdatedEvent event = new ApplicationStatusUpdatedEvent(
                12L,
                "student-1",
                "employer-1",
                91L,
                "SUBMITTED",
                "UNDER_REVIEW",
                Instant.parse("2026-04-12T18:01:00Z")
        );

        publisher.publishStatusUpdated(event);

        verify(rabbitTemplate).convertAndSend("jobportal.domain.events", "application.status-changed", event);
    }

    @Test
    void publishWithdrawn_ShouldUseWithdrawnRoutingKey() {
        ApplicationWithdrawnEvent event = new ApplicationWithdrawnEvent(
                13L,
                "student-1",
                91L,
                Instant.parse("2026-04-12T18:02:00Z")
        );

        publisher.publishWithdrawn(event);

        verify(rabbitTemplate).convertAndSend("jobportal.domain.events", "application.withdrawn", event);
    }
}
