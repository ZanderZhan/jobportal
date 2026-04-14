package com.jobportal.notificationservice.service;

import com.jobportal.notificationservice.config.NotificationRetryProperties;
import com.jobportal.notificationservice.entity.DeliveryChannel;
import com.jobportal.notificationservice.entity.DeliveryRecord;
import com.jobportal.notificationservice.entity.DeliveryStatus;
import com.jobportal.notificationservice.entity.Notification;
import com.jobportal.notificationservice.entity.NotificationPreference;
import com.jobportal.notificationservice.entity.NotificationStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
public class NotificationDeliveryService {

    private static final String RECIPIENT_PENDING_MESSAGE = "Recipient email is not ready yet.";

    private final EmailSender emailSender;
    private final NotificationRetryProperties retryProperties;

    public NotificationDeliveryService(EmailSender emailSender, NotificationRetryProperties retryProperties) {
        this.emailSender = emailSender;
        this.retryProperties = retryProperties;
    }

    public NotificationDeliveryResult deliverNew(Notification notification, NotificationPreference preference) {
        ensureInAppDelivery(notification, preference);
        NotificationDeliveryResult result = continueEmailDelivery(notification, preference);
        refreshStatus(notification);
        return result;
    }

    public NotificationDeliveryResult continueEmailDelivery(Notification notification, NotificationPreference preference) {
        if (!StringUtils.hasText(notification.getEmailSubject()) || !StringUtils.hasText(notification.getEmailBody())) {
            refreshStatus(notification);
            return new NotificationDeliveryResult(false);
        }

        if (hasDeliveryRecord(notification, DeliveryChannel.EMAIL, DeliveryStatus.SENT)) {
            refreshStatus(notification);
            return new NotificationDeliveryResult(false);
        }

        if (!preference.isEmailEnabled()) {
            if (!hasChannelRecord(notification, DeliveryChannel.EMAIL)) {
                createDeliveryRecord(notification, DeliveryChannel.EMAIL, DeliveryStatus.SKIPPED, "User disabled email notifications.", "preference-skip");
            }
            refreshStatus(notification);
            return new NotificationDeliveryResult(false);
        }

        NotificationDeliveryResult result = attemptEmailDelivery(notification);
        refreshStatus(notification);
        return result;
    }

    public NotificationDeliveryResult retryEmail(Notification notification) {
        if (!StringUtils.hasText(notification.getEmailSubject()) || !StringUtils.hasText(notification.getEmailBody())) {
            refreshStatus(notification);
            return new NotificationDeliveryResult(false);
        }

        NotificationDeliveryResult result = attemptEmailDelivery(notification);
        refreshStatus(notification);
        return result;
    }

    public void scheduleRecipientRecovery(Notification notification) {
        notification.setNextRetryAt(Instant.now().plus(retryProperties.recipientRecoveryMinutes(), ChronoUnit.MINUTES));
        notification.setStatus(NotificationStatus.PENDING_RECIPIENT);
    }

    private void ensureInAppDelivery(Notification notification, NotificationPreference preference) {
        if (hasChannelRecord(notification, DeliveryChannel.IN_APP)) {
            return;
        }

        if (preference.isInAppEnabled()) {
            createDeliveryRecord(notification, DeliveryChannel.IN_APP, DeliveryStatus.SENT, null, "in-app-store");
        } else {
            createDeliveryRecord(notification, DeliveryChannel.IN_APP, DeliveryStatus.SKIPPED, "User disabled in-app notifications.", "preference-skip");
        }
    }

    private NotificationDeliveryResult attemptEmailDelivery(Notification notification) {
        notification.setLastDeliveryAttemptAt(Instant.now());

        if (!StringUtils.hasText(notification.getRecipientEmail())) {
            createDeliveryRecord(notification, DeliveryChannel.EMAIL, DeliveryStatus.PENDING, RECIPIENT_PENDING_MESSAGE, "recipient-cache");
            notification.setLastDeliveryError(RECIPIENT_PENDING_MESSAGE);
            notification.setNextRetryAt(null);
            return new NotificationDeliveryResult(true);
        }

        notification.setEmailAttemptCount(notification.getEmailAttemptCount() + 1);

        try {
            emailSender.send(notification.getRecipientEmail(), notification.getEmailSubject(), notification.getEmailBody());
            createDeliveryRecord(notification, DeliveryChannel.EMAIL, DeliveryStatus.SENT, null, "smtp-javamail");
            notification.setLastDeliveryError(null);
            notification.setNextRetryAt(null);
        } catch (Exception ex) {
            createDeliveryRecord(notification, DeliveryChannel.EMAIL, DeliveryStatus.FAILED, ex.getMessage(), "smtp-javamail");
            notification.setLastDeliveryError(ex.getMessage());

            if (notification.getEmailAttemptCount() < retryProperties.maxEmailAttempts()) {
                notification.setNextRetryAt(Instant.now().plus(retryProperties.backoffMinutes(), ChronoUnit.MINUTES));
            } else {
                notification.setNextRetryAt(null);
            }
        }

        return new NotificationDeliveryResult(false);
    }

    private void refreshStatus(Notification notification) {
        boolean hasInAppSuccess = hasDeliveryRecord(notification, DeliveryChannel.IN_APP, DeliveryStatus.SENT);
        boolean hasEmailSuccess = hasDeliveryRecord(notification, DeliveryChannel.EMAIL, DeliveryStatus.SENT);
        boolean hasEmailFailure = hasDeliveryRecord(notification, DeliveryChannel.EMAIL, DeliveryStatus.FAILED);
        boolean hasPendingRecipient = hasDeliveryRecord(notification, DeliveryChannel.EMAIL, DeliveryStatus.PENDING);
        boolean hasEmailRetryScheduled = hasEmailFailure && notification.getNextRetryAt() != null;
        boolean inAppSkipped = hasDeliveryRecord(notification, DeliveryChannel.IN_APP, DeliveryStatus.SKIPPED);
        boolean emailSkipped = hasDeliveryRecord(notification, DeliveryChannel.EMAIL, DeliveryStatus.SKIPPED)
                || !StringUtils.hasText(notification.getEmailSubject());

        if (!hasInAppSuccess && !hasEmailSuccess && inAppSkipped && emailSkipped) {
            notification.setStatus(NotificationStatus.SUPPRESSED);
            return;
        }

        if (hasPendingRecipient && !hasEmailSuccess) {
            notification.setStatus(NotificationStatus.PENDING_RECIPIENT);
            return;
        }

        if (hasEmailRetryScheduled) {
            notification.setStatus(NotificationStatus.RETRY_SCHEDULED);
            return;
        }

        if (hasInAppSuccess && (hasEmailSuccess || emailSkipped)) {
            notification.setStatus(NotificationStatus.DELIVERED);
            return;
        }

        if (hasEmailSuccess && !hasInAppSuccess) {
            notification.setStatus(NotificationStatus.DELIVERED);
            return;
        }

        if (hasInAppSuccess && hasEmailFailure) {
            notification.setStatus(NotificationStatus.PARTIALLY_DELIVERED);
            return;
        }

        if (hasEmailFailure) {
            notification.setStatus(NotificationStatus.FAILED);
            return;
        }

        notification.setStatus(NotificationStatus.CREATED);
    }

    private boolean hasChannelRecord(Notification notification, DeliveryChannel channel) {
        return notification.getDeliveryRecords().stream()
                .anyMatch(record -> record.getChannel() == channel);
    }

    private boolean hasDeliveryRecord(Notification notification, DeliveryChannel channel, DeliveryStatus status) {
        return notification.getDeliveryRecords().stream()
                .anyMatch(record -> record.getChannel() == channel && record.getStatus() == status);
    }

    private void createDeliveryRecord(
            Notification notification,
            DeliveryChannel channel,
            DeliveryStatus status,
            String errorMessage,
            String provider
    ) {
        DeliveryRecord deliveryRecord = new DeliveryRecord();
        deliveryRecord.setChannel(channel);
        deliveryRecord.setAttemptNo(notification.getDeliveryRecords().size() + 1);
        deliveryRecord.setProvider(provider);
        deliveryRecord.setStatus(status);
        deliveryRecord.setErrorMessage(errorMessage);
        deliveryRecord.setSentAt(Instant.now());
        notification.addDeliveryRecord(deliveryRecord);
    }
}
