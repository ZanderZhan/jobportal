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
        notification.setStatus(NotificationStatus.PENDING);
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
        if (notification.getStatus() == NotificationStatus.FAILED
                && notification.getRetryCount() >= retryProperties.maxEmailAttempts()) {
            notification.setNextRetryAt(null);
            return new NotificationDeliveryResult(false);
        }

        notification.setLastDeliveryAttemptAt(Instant.now());
        notification.setLastAttemptedAt(notification.getLastDeliveryAttemptAt());

        if (!StringUtils.hasText(notification.getRecipientEmail())) {
            createDeliveryRecord(notification, DeliveryChannel.EMAIL, DeliveryStatus.PENDING, RECIPIENT_PENDING_MESSAGE, "recipient-cache");
            notification.setLastDeliveryError(RECIPIENT_PENDING_MESSAGE);
            notification.setFailureReason(RECIPIENT_PENDING_MESSAGE);
            notification.setNextRetryAt(null);
            notification.setStatus(NotificationStatus.PENDING);
            return new NotificationDeliveryResult(true);
        }

        notification.setEmailAttemptCount(notification.getEmailAttemptCount() + 1);

        try {
            EmailSendResult sendResult = emailSender.send(notification.getRecipientEmail(), notification.getEmailSubject(), notification.getEmailBody());
            applyEmailResult(notification, sendResult);
        } catch (Exception ex) {
            applyEmailResult(notification, EmailSendResult.permanentFailure(ex.getMessage()));
        }

        return new NotificationDeliveryResult(false);
    }

    private void applyEmailResult(Notification notification, EmailSendResult sendResult) {
        if (sendResult.successful()) {
            createDeliveryRecord(notification, DeliveryChannel.EMAIL, DeliveryStatus.SENT, null, "email-sender");
            notification.setLastDeliveryError(null);
            notification.setFailureReason(null);
            notification.setNextRetryAt(null);
            notification.setStatus(NotificationStatus.SENT);
            return;
        }

        String failureMessage = StringUtils.hasText(sendResult.message()) ? sendResult.message() : sendResult.status().name();
        createDeliveryRecord(notification, DeliveryChannel.EMAIL, DeliveryStatus.FAILED, failureMessage, "email-sender");
        notification.setRetryCount(notification.getRetryCount() + 1);
        notification.setLastDeliveryError(failureMessage);
        notification.setFailureReason(failureMessage);

        boolean retryable = sendResult.status() == EmailSendStatus.TEMPORARY_FAILURE || sendResult.status() == EmailSendStatus.TIMEOUT;
        if (retryable && notification.getRetryCount() < retryProperties.maxEmailAttempts()) {
            notification.setNextRetryAt(Instant.now().plus(retryProperties.backoffMinutes(), ChronoUnit.MINUTES));
            notification.setStatus(NotificationStatus.RETRYING);
            return;
        }

        notification.setNextRetryAt(null);
        notification.setStatus(NotificationStatus.FAILED);
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
            notification.setStatus(NotificationStatus.SENT);
            return;
        }

        if (hasPendingRecipient && !hasEmailSuccess) {
            notification.setStatus(NotificationStatus.PENDING);
            return;
        }

        if (hasEmailRetryScheduled) {
            notification.setStatus(NotificationStatus.RETRYING);
            return;
        }

        if (hasInAppSuccess && (hasEmailSuccess || emailSkipped)) {
            notification.setStatus(NotificationStatus.SENT);
            return;
        }

        if (hasEmailSuccess && !hasInAppSuccess) {
            notification.setStatus(NotificationStatus.SENT);
            return;
        }

        if (hasInAppSuccess && hasEmailFailure) {
            notification.setStatus(notification.getNextRetryAt() != null ? NotificationStatus.RETRYING : NotificationStatus.FAILED);
            return;
        }

        if (hasEmailFailure) {
            notification.setStatus(NotificationStatus.FAILED);
            return;
        }

        notification.setStatus(NotificationStatus.PENDING);
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
