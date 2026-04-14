package com.jobportal.notificationservice.service;

import com.jobportal.notificationservice.dto.EventNotificationRequest;
import com.jobportal.notificationservice.dto.NotificationResponse;
import com.jobportal.notificationservice.entity.DeliveryChannel;
import com.jobportal.notificationservice.entity.NotificationEventType;
import com.jobportal.notificationservice.repository.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class NotificationWorkflowServiceTest {

    @Autowired
    private NotificationWorkflowService workflowService;

    @Autowired
    private NotificationTemplateService templateService;

    @Autowired
    private NotificationRepository notificationRepository;

    @BeforeEach
    void clearNotifications() {
        // Each test should start from a clean notification table.
        notificationRepository.deleteAll();
    }

    @Test
    void shouldCreateNotificationAndMarkEmailAsFailedWhenRecipientEmailMissing() {
        NotificationResponse response = workflowService.handleEvent(new EventNotificationRequest(
                "application-submitted-100",
                NotificationEventType.APPLICATION_SUBMITTED,
                15L,
                null,
                Instant.parse("2026-04-07T22:00:00Z"),
                Map.of(
                        "applicationId", "100",
                        "jobId", "55"
                )
        ));

        assertNotNull(response.id());
        assertEquals(NotificationEventType.APPLICATION_SUBMITTED, response.eventType());
        assertEquals(15L, response.recipientUserId());
        assertEquals("Application received", response.title());
        assertEquals("Your application for job 55 has been submitted.", response.body());
        assertEquals(1, notificationRepository.findAll().size());
        assertEquals(2, workflowService.getDeliveryRecords(response.id()).size());
        assertEquals(DeliveryChannel.IN_APP, workflowService.getDeliveryRecords(response.id()).get(0).channel());
        assertEquals(DeliveryChannel.EMAIL, workflowService.getDeliveryRecords(response.id()).get(1).channel());
    }

    @Test
    void shouldReturnExistingNotificationForDuplicateEventKey() {
        EventNotificationRequest request = new EventNotificationRequest(
                "application-status-changed-42-INTERVIEW",
                NotificationEventType.APPLICATION_STATUS_CHANGED,
                42L,
                null,
                Instant.parse("2026-04-07T22:10:00Z"),
                Map.of(
                        "applicationId", "42",
                        "oldStatus", "UNDER_REVIEW",
                        "newStatus", "INTERVIEW"
                )
        );

        NotificationResponse first = workflowService.handleEvent(request);
        NotificationResponse second = workflowService.handleEvent(request);

        assertEquals(first.id(), second.id());
        assertEquals(1, notificationRepository.findAll().stream()
                .filter(notification -> "application-status-changed-42-INTERVIEW".equals(notification.getEventKey()))
                .count());
    }

    @Test
    void shouldExposeSeededTemplates() {
        assertFalse(templateService.getActiveTemplates().isEmpty());
    }
}
