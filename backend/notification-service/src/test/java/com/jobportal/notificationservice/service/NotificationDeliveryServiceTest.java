package com.jobportal.notificationservice.service;

import com.jobportal.notificationservice.config.NotificationRetryProperties;
import com.jobportal.notificationservice.entity.Notification;
import com.jobportal.notificationservice.entity.NotificationEventType;
import com.jobportal.notificationservice.entity.NotificationPreference;
import com.jobportal.notificationservice.entity.NotificationStatus;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class NotificationDeliveryServiceTest {

    private static final int MAX_EMAIL_ATTEMPTS = 3;

    private final MockEmailSender emailSender = new MockEmailSender();
    private final NotificationDeliveryService deliveryService = new NotificationDeliveryService(
            emailSender,
            new NotificationRetryProperties(10, 5, MAX_EMAIL_ATTEMPTS, 2, 15, 600000)
    );

    @Test
    void shouldMovePendingNotificationToSentWhenEmailSucceeds() {
        Notification notification = notification();
        emailSender.simulateSuccess();

        deliveryService.continueEmailDelivery(notification, emailPreference());

        assertEquals(NotificationStatus.SENT, notification.getStatus());
        assertEquals(0, notification.getRetryCount());
        assertNull(notification.getFailureReason());
        assertNull(notification.getNextRetryAt());
        assertNotNull(notification.getLastAttemptedAt());
    }

    @Test
    void shouldMovePendingNotificationToFailedWhenPermanentFailureOccurs() {
        Notification notification = notification();
        emailSender.simulatePermanentFailure("mailbox does not exist");

        deliveryService.continueEmailDelivery(notification, emailPreference());

        assertEquals(NotificationStatus.FAILED, notification.getStatus());
        assertEquals(1, notification.getRetryCount());
        assertEquals("mailbox does not exist", notification.getFailureReason());
        assertNull(notification.getNextRetryAt());
    }

    @Test
    void shouldIncrementRetryCountAndMarkRetryingWhenTemporaryFailureOccurs() {
        Notification notification = notification();
        emailSender.simulateTemporaryFailure("provider unavailable");

        deliveryService.continueEmailDelivery(notification, emailPreference());

        assertEquals(NotificationStatus.RETRYING, notification.getStatus());
        assertEquals(1, notification.getRetryCount());
        assertEquals("provider unavailable", notification.getFailureReason());
        assertNotNull(notification.getNextRetryAt());
    }

    @Test
    void shouldStayFailedAfterMaxRetryIsReached() {
        Notification notification = notification();
        emailSender.simulateTemporaryFailure("provider unavailable");

        deliveryService.continueEmailDelivery(notification, emailPreference());
        deliveryService.retryEmail(notification);
        deliveryService.retryEmail(notification);

        assertEquals(NotificationStatus.FAILED, notification.getStatus());
        assertEquals(MAX_EMAIL_ATTEMPTS, notification.getRetryCount());
        assertNull(notification.getNextRetryAt());

        deliveryService.retryEmail(notification);

        assertEquals(NotificationStatus.FAILED, notification.getStatus());
        assertEquals(MAX_EMAIL_ATTEMPTS, notification.getRetryCount());
        assertNull(notification.getNextRetryAt());
    }

    private Notification notification() {
        Notification notification = new Notification();
        notification.setEventKey("event-1");
        notification.setEventType(NotificationEventType.APPLICATION_SUBMITTED);
        notification.setRecipientUserId("student-1");
        notification.setRecipientEmail("student@example.com");
        notification.setRecipientName("Student One");
        notification.setTitle("Application received");
        notification.setBody("Your application was received.");
        notification.setEmailSubject("Application received");
        notification.setEmailBody("Your application was received.");
        notification.setStatus(NotificationStatus.PENDING);
        notification.setCreatedAt(Instant.parse("2026-04-07T22:00:00Z"));
        return notification;
    }

    private NotificationPreference emailPreference() {
        NotificationPreference preference = new NotificationPreference();
        preference.setEventType(NotificationEventType.APPLICATION_SUBMITTED);
        preference.setInAppEnabled(false);
        preference.setEmailEnabled(true);
        return preference;
    }
}
