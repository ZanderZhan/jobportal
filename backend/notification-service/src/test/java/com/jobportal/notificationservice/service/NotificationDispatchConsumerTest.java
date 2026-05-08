package com.jobportal.notificationservice.service;

import com.jobportal.notificationservice.dto.NotificationDispatchEvent;
import com.jobportal.notificationservice.entity.Notification;
import com.jobportal.notificationservice.entity.NotificationEventType;
import com.jobportal.notificationservice.entity.NotificationPreference;
import com.jobportal.notificationservice.entity.NotificationStatus;
import com.jobportal.notificationservice.repository.NotificationRepository;
import com.rabbitmq.client.Channel;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;

import java.io.IOException;
import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NotificationDispatchConsumerTest {

    @Test
    void shouldDropDuplicateDispatchEventWhenNotificationAlreadySent() throws IOException {
        NotificationRepository notificationRepository = mock(NotificationRepository.class);
        NotificationDeliveryService notificationDeliveryService = mock(NotificationDeliveryService.class);
        NotificationPreferenceService notificationPreferenceService = mock(NotificationPreferenceService.class);
        Channel channel = mock(Channel.class);
        NotificationDispatchConsumer consumer = new NotificationDispatchConsumer(
                notificationRepository,
                notificationDeliveryService,
                notificationPreferenceService
        );
        Notification notification = notification(NotificationStatus.SENT, 0);
        when(notificationRepository.findById(101L)).thenReturn(Optional.of(notification));

        consumer.onNotificationDispatch(dispatchEvent(), message(22L), channel);

        verify(notificationDeliveryService, never()).continueEmailDelivery(any(), any());
        verify(channel).basicAck(22L, false);
    }

    @Test
    void shouldNackAndRequeueTemporaryFailureBeforeMaxRetry() throws IOException {
        NotificationRepository notificationRepository = mock(NotificationRepository.class);
        NotificationDeliveryService notificationDeliveryService = mock(NotificationDeliveryService.class);
        NotificationPreferenceService notificationPreferenceService = mock(NotificationPreferenceService.class);
        Channel channel = mock(Channel.class);
        NotificationDispatchConsumer consumer = new NotificationDispatchConsumer(
                notificationRepository,
                notificationDeliveryService,
                notificationPreferenceService
        );
        Notification notification = notification(NotificationStatus.PENDING, 1);
        NotificationPreference preference = preference();
        when(notificationRepository.findById(101L)).thenReturn(Optional.of(notification));
        when(notificationPreferenceService.resolvePreference("student-15", NotificationEventType.APPLICATION_SUBMITTED))
                .thenReturn(preference);
        doAnswer(invocation -> {
            notification.setRetryCount(2);
            notification.setStatus(NotificationStatus.RETRYING);
            notification.setFailureReason("smtp unavailable");
            return new NotificationDeliveryResult(false);
        }).when(notificationDeliveryService).continueEmailDelivery(notification, preference);

        consumer.onNotificationDispatch(dispatchEvent(), message(23L), channel);

        assertEquals(2, notification.getRetryCount());
        assertEquals(NotificationStatus.RETRYING, notification.getStatus());
        assertEquals("smtp unavailable", notification.getFailureReason());
        verify(channel).basicNack(23L, false, true);
        verify(channel, never()).basicAck(23L, false);
    }

    @Test
    void shouldAckAndMarkFailedWhenMaxRetryReached() throws IOException {
        NotificationRepository notificationRepository = mock(NotificationRepository.class);
        NotificationDeliveryService notificationDeliveryService = mock(NotificationDeliveryService.class);
        NotificationPreferenceService notificationPreferenceService = mock(NotificationPreferenceService.class);
        Channel channel = mock(Channel.class);
        NotificationDispatchConsumer consumer = new NotificationDispatchConsumer(
                notificationRepository,
                notificationDeliveryService,
                notificationPreferenceService
        );
        Notification notification = notification(NotificationStatus.PENDING, 2);
        NotificationPreference preference = preference();
        when(notificationRepository.findById(101L)).thenReturn(Optional.of(notification));
        when(notificationPreferenceService.resolvePreference("student-15", NotificationEventType.APPLICATION_SUBMITTED))
                .thenReturn(preference);
        doAnswer(invocation -> {
            notification.setRetryCount(3);
            notification.setStatus(NotificationStatus.FAILED);
            notification.setFailureReason("smtp unavailable");
            return new NotificationDeliveryResult(false);
        }).when(notificationDeliveryService).continueEmailDelivery(notification, preference);

        consumer.onNotificationDispatch(dispatchEvent(), message(24L), channel);

        assertEquals(3, notification.getRetryCount());
        assertEquals(NotificationStatus.FAILED, notification.getStatus());
        assertEquals("smtp unavailable", notification.getFailureReason());
        verify(channel).basicAck(24L, false);
        verify(channel, never()).basicNack(24L, false, true);
    }

    @Test
    void shouldAckAndMarkFailedForPermanentFailure() throws IOException {
        NotificationRepository notificationRepository = mock(NotificationRepository.class);
        NotificationDeliveryService notificationDeliveryService = mock(NotificationDeliveryService.class);
        NotificationPreferenceService notificationPreferenceService = mock(NotificationPreferenceService.class);
        Channel channel = mock(Channel.class);
        NotificationDispatchConsumer consumer = new NotificationDispatchConsumer(
                notificationRepository,
                notificationDeliveryService,
                notificationPreferenceService
        );
        Notification notification = notification(NotificationStatus.PENDING, 0);
        NotificationPreference preference = preference();
        when(notificationRepository.findById(101L)).thenReturn(Optional.of(notification));
        when(notificationPreferenceService.resolvePreference("student-15", NotificationEventType.APPLICATION_SUBMITTED))
                .thenReturn(preference);
        doAnswer(invocation -> {
            notification.setStatus(NotificationStatus.FAILED);
            notification.setFailureReason("missing email");
            return new NotificationDeliveryResult(false);
        }).when(notificationDeliveryService).continueEmailDelivery(notification, preference);

        consumer.onNotificationDispatch(dispatchEvent(), message(25L), channel);

        assertEquals(NotificationStatus.FAILED, notification.getStatus());
        assertEquals("missing email", notification.getFailureReason());
        verify(channel).basicAck(25L, false);
        verify(channel, never()).basicNack(25L, false, true);
    }

    private Notification notification(NotificationStatus status, int retryCount) {
        Notification notification = new Notification();
        notification.setStatus(status);
        notification.setRetryCount(retryCount);
        notification.setRecipientUserId("student-15");
        notification.setEventType(NotificationEventType.APPLICATION_SUBMITTED);
        return notification;
    }

    private NotificationPreference preference() {
        NotificationPreference preference = new NotificationPreference();
        preference.setEventType(NotificationEventType.APPLICATION_SUBMITTED);
        preference.setInAppEnabled(true);
        preference.setEmailEnabled(true);
        return preference;
    }

    private NotificationDispatchEvent dispatchEvent() {
        return new NotificationDispatchEvent(
                101L,
                "student-15",
                NotificationEventType.APPLICATION_SUBMITTED,
                Instant.parse("2026-04-07T22:00:00Z")
        );
    }

    private Message message(long deliveryTag) {
        MessageProperties messageProperties = new MessageProperties();
        messageProperties.setDeliveryTag(deliveryTag);
        return new Message(new byte[0], messageProperties);
    }
}
