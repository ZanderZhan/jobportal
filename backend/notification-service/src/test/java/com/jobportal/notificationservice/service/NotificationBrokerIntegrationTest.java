package com.jobportal.notificationservice.service;

import com.jobportal.notificationservice.NotificationTestConfiguration;
import com.jobportal.notificationservice.dto.EventNotificationRequest;
import com.jobportal.notificationservice.dto.NotificationDispatchEvent;
import com.jobportal.notificationservice.dto.NotificationResponse;
import com.jobportal.notificationservice.entity.NotificationEventType;
import com.jobportal.notificationservice.entity.NotificationStatus;
import com.jobportal.notificationservice.repository.NotificationPreferenceRepository;
import com.jobportal.notificationservice.repository.NotificationRepository;
import com.rabbitmq.client.Channel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@SpringBootTest
@Import(NotificationTestConfiguration.class)
@ActiveProfiles("test")
class NotificationBrokerIntegrationTest {

    private static final int BURST_SIZE = 120;
    private static final int BURST_SUCCESS_COUNT = 100;
    private static final int BURST_FAILURE_COUNT = BURST_SIZE - BURST_SUCCESS_COUNT;

    @Autowired
    private NotificationWorkflowService workflowService;

    @Autowired
    private NotificationDispatchConsumer dispatchConsumer;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private NotificationPreferenceRepository notificationPreferenceRepository;

    @Autowired
    private NotificationTestConfiguration.CapturingNotificationDispatchPublisher dispatchPublisher;

    @Autowired
    private NotificationTestConfiguration.ControllableEmailSender emailSender;

    @BeforeEach
    void setUp() {
        notificationRepository.deleteAll();
        notificationPreferenceRepository.deleteAll();
        dispatchPublisher.clear();
        emailSender.simulateSuccess();
    }

    @Test
    void shouldCreateQueueConsumeAndMarkNotificationSent() throws IOException {
        NotificationResponse response = workflowService.handleEvent(request("e2e-success-1", "student-1", "student1@example.com"));

        assertEquals(NotificationStatus.PENDING, response.status());
        assertEquals(1, dispatchPublisher.events().size());

        Channel channel = mock(Channel.class);
        dispatchConsumer.onNotificationDispatch(dispatchPublisher.events().getFirst(), message(1L), channel);

        assertEquals(NotificationStatus.SENT, notificationRepository.findById(response.id()).orElseThrow().getStatus());
        verify(channel).basicAck(1L, false);
    }

    @Test
    void shouldRequeueTemporaryFailureAndStopAtFailedAfterMaxRetries() throws IOException {
        NotificationResponse response = workflowService.handleEvent(request("e2e-temp-failure-1", "student-2", "student2@example.com"));
        NotificationDispatchEvent event = dispatchPublisher.events().getFirst();
        Channel channel = mock(Channel.class);

        emailSender.simulateTemporaryFailure("smtp unavailable");
        dispatchConsumer.onNotificationDispatch(event, message(10L), channel);
        dispatchConsumer.onNotificationDispatch(event, message(11L), channel);
        dispatchConsumer.onNotificationDispatch(event, message(12L), channel);

        var notification = notificationRepository.findById(response.id()).orElseThrow();
        assertEquals(NotificationStatus.FAILED, notification.getStatus());
        assertEquals(3, notification.getRetryCount());
        assertEquals("smtp unavailable", notification.getFailureReason());
        verify(channel).basicNack(10L, false, true);
        verify(channel).basicNack(11L, false, true);
        verify(channel).basicAck(12L, false);
    }

    @Test
    void shouldHandleBurstNotificationsWithExactSentAndFailedCounts() throws IOException {
        for (int index = 0; index < BURST_SIZE; index++) {
            workflowService.handleEvent(request(
                    "burst-" + index,
                    "student-burst-" + index,
                    "student-burst-" + index + "@example.com"
            ));
        }

        assertEquals(BURST_SIZE, dispatchPublisher.events().size());

        Channel channel = mock(Channel.class);
        for (int index = 0; index < dispatchPublisher.events().size(); index++) {
            if (index < BURST_SUCCESS_COUNT) {
                emailSender.simulateSuccess();
            } else {
                emailSender.simulatePermanentFailure("invalid recipient");
            }
            dispatchConsumer.onNotificationDispatch(dispatchPublisher.events().get(index), message(1000L + index), channel);
        }

        assertEquals(BURST_SUCCESS_COUNT, notificationRepository.countByStatus(NotificationStatus.SENT));
        assertEquals(BURST_FAILURE_COUNT, notificationRepository.countByStatus(NotificationStatus.FAILED));
        assertEquals(0, notificationRepository.countByStatus(NotificationStatus.PENDING));
        verify(channel, times(BURST_SIZE)).basicAck(org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.eq(false));
    }

    private EventNotificationRequest request(String eventKey, String recipientUserId, String recipientEmail) {
        return new EventNotificationRequest(
                eventKey,
                NotificationEventType.APPLICATION_SUBMITTED,
                recipientUserId,
                recipientEmail,
                "Student",
                Instant.parse("2026-04-07T22:00:00Z"),
                Map.of(
                        "applicationId", eventKey,
                        "jobId", "55"
                )
        );
    }

    private Message message(long deliveryTag) {
        MessageProperties messageProperties = new MessageProperties();
        messageProperties.setDeliveryTag(deliveryTag);
        return new Message(new byte[0], messageProperties);
    }
}
