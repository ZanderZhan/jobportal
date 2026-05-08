package com.jobportal.notificationservice.service;

import com.jobportal.notificationservice.NotificationTestConfiguration;
import com.jobportal.notificationservice.dto.EventNotificationRequest;
import com.jobportal.notificationservice.dto.NotificationResponse;
import com.jobportal.notificationservice.dto.UpdateNotificationPreferenceRequest;
import com.jobportal.notificationservice.entity.NotificationEventType;
import com.jobportal.notificationservice.entity.NotificationStatus;
import com.jobportal.notificationservice.repository.NotificationPreferenceRepository;
import com.jobportal.notificationservice.repository.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
@Import(NotificationTestConfiguration.class)
@ActiveProfiles("test")
class NotificationWorkflowServiceTest {

    @Autowired
    private NotificationWorkflowService workflowService;

    @Autowired
    private NotificationTemplateService templateService;

    @Autowired
    private NotificationPreferenceService notificationPreferenceService;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private NotificationPreferenceRepository notificationPreferenceRepository;

    @Autowired
    private NotificationTestConfiguration.CapturingNotificationDispatchPublisher notificationDispatchPublisher;

    @BeforeEach
    void clearNotifications() {
        notificationRepository.deleteAll();
        notificationPreferenceRepository.deleteAll();
        notificationDispatchPublisher.clear();
    }

    @Test
    void shouldCreatePendingNotificationAndPublishDispatchEvent() {
        NotificationResponse response = workflowService.handleEvent(new EventNotificationRequest(
                "application-submitted-100",
                NotificationEventType.APPLICATION_SUBMITTED,
                "student-15",
                null,
                "Wenkai Zhu",
                Instant.parse("2026-04-07T22:00:00Z"),
                Map.of(
                        "applicationId", "100",
                        "jobId", "55"
                )
        ));

        assertNotNull(response.id());
        assertEquals(NotificationEventType.APPLICATION_SUBMITTED, response.eventType());
        assertEquals("student-15", response.recipientUserId());
        assertEquals("Wenkai Zhu", response.recipientName());
        assertEquals("Application received", response.title());
        assertEquals(NotificationStatus.PENDING, response.status());
        assertEquals(false, response.actionRequired());
        assertEquals(1, notificationRepository.findAll().size());
        assertEquals(1, notificationDispatchPublisher.events().size());
        assertEquals(response.id(), notificationDispatchPublisher.events().getFirst().notificationId());
        assertEquals("student-15", notificationDispatchPublisher.events().getFirst().recipientId());
        assertEquals(NotificationEventType.APPLICATION_SUBMITTED, notificationDispatchPublisher.events().getFirst().type());
    }

    @Test
    void shouldReturnExistingNotificationForDuplicateEventKey() {
        EventNotificationRequest request = new EventNotificationRequest(
                "application-status-changed-42-INTERVIEW",
                NotificationEventType.APPLICATION_STATUS_CHANGED,
                "student-42",
                "student42@example.com",
                "Student Forty Two",
                Instant.parse("2026-04-07T22:10:00Z"),
                Map.of(
                        "applicationId", "42",
                        "jobId", "77",
                        "employerId", "employer-9",
                        "oldStatus", "UNDER_REVIEW",
                        "newStatus", "INTERVIEW"
                )
        );

        NotificationResponse first = workflowService.handleEvent(request);
        NotificationResponse second = workflowService.handleEvent(request);

        assertEquals(first.id(), second.id());
        assertEquals(true, first.actionRequired());
        assertEquals(1, notificationRepository.findAll().stream()
                .filter(notification -> "application-status-changed-42-INTERVIEW".equals(notification.getEventKey()))
                .count());
    }

    @Test
    void shouldDeferEmailPreferenceHandlingToDispatchConsumer() {
        notificationPreferenceService.updatePreference(
                "student-88",
                NotificationEventType.APPLICATION_WITHDRAWN,
                new UpdateNotificationPreferenceRequest(true, false)
        );

        NotificationResponse response = workflowService.handleEvent(new EventNotificationRequest(
                "application-withdrawn-88",
                NotificationEventType.APPLICATION_WITHDRAWN,
                "student-88",
                "student88@example.com",
                "Student Eighty Eight",
                Instant.parse("2026-04-08T10:00:00Z"),
                Map.of(
                        "applicationId", "88",
                        "jobId", "12"
                )
        ));

        assertEquals(NotificationStatus.PENDING, response.status());
        assertEquals(1, notificationDispatchPublisher.events().size());
        assertEquals(response.id(), notificationDispatchPublisher.events().getFirst().notificationId());
    }

    @Test
    void shouldPublishDispatchEventInsteadOfSendingSynchronouslyOnManualRetry() {
        NotificationResponse response = workflowService.handleEvent(new EventNotificationRequest(
                "application-submitted-retry-101",
                NotificationEventType.APPLICATION_SUBMITTED,
                "student-15",
                "student15@example.com",
                "Student Fifteen",
                Instant.parse("2026-04-07T22:00:00Z"),
                Map.of(
                        "applicationId", "101",
                        "jobId", "55"
                )
        ));
        notificationDispatchPublisher.clear();

        NotificationResponse retried = workflowService.retry(response.id());

        assertEquals(NotificationStatus.PENDING, retried.status());
        assertEquals(1, notificationDispatchPublisher.events().size());
        assertEquals(response.id(), notificationDispatchPublisher.events().getFirst().notificationId());
        assertEquals("student-15", notificationDispatchPublisher.events().getFirst().recipientId());
    }

    @Test
    void shouldExposeSeededTemplates() {
        assertFalse(templateService.getActiveTemplates().isEmpty());
    }
}
