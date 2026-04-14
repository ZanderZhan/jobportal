package com.jobportal.notificationservice.service;

import com.jobportal.notificationservice.dto.DeliveryRecordResponse;
import com.jobportal.notificationservice.dto.EventNotificationRequest;
import com.jobportal.notificationservice.dto.NotificationResponse;
import com.jobportal.notificationservice.entity.DeliveryChannel;
import com.jobportal.notificationservice.entity.DeliveryRecord;
import com.jobportal.notificationservice.entity.DeliveryStatus;
import com.jobportal.notificationservice.entity.Notification;
import com.jobportal.notificationservice.entity.NotificationStatus;
import com.jobportal.notificationservice.entity.NotificationTemplate;
import com.jobportal.notificationservice.exception.NotificationNotFoundException;
import com.jobportal.notificationservice.repository.DeliveryRecordRepository;
import com.jobportal.notificationservice.repository.NotificationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@Transactional
public class NotificationWorkflowService {

    private final NotificationRepository notificationRepository;
    private final DeliveryRecordRepository deliveryRecordRepository;
    private final NotificationTemplateService templateService;
    private final TemplateRenderingService renderingService;
    private final EmailSender emailSender;

    public NotificationWorkflowService(
            NotificationRepository notificationRepository,
            DeliveryRecordRepository deliveryRecordRepository,
            NotificationTemplateService templateService,
            TemplateRenderingService renderingService,
            EmailSender emailSender
    ) {
        this.notificationRepository = notificationRepository;
        this.deliveryRecordRepository = deliveryRecordRepository;
        this.templateService = templateService;
        this.renderingService = renderingService;
        this.emailSender = emailSender;
    }

    public NotificationResponse handleEvent(EventNotificationRequest request) {
        return notificationRepository.findByEventKey(request.eventKey())
                .map(this::toResponse)
                .orElseGet(() -> processNewNotification(request));
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> getNotificationsForRecipient(Long recipientUserId) {
        return notificationRepository.findByRecipientUserIdOrderByCreatedAtDesc(recipientUserId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> getFailedNotifications() {
        return notificationRepository.findByStatusOrderByCreatedAtDesc(NotificationStatus.FAILED).stream()
                .map(this::toResponse)
                .toList();
    }

    public NotificationResponse markAsRead(Long notificationId, Long recipientUserId) {
        Notification notification = getOwnedNotification(notificationId, recipientUserId);
        notification.setRead(true);
        notification.setReadAt(Instant.now());
        return toResponse(notification);
    }

    public void markAllAsRead(Long recipientUserId) {
        notificationRepository.markAllReadForRecipient(recipientUserId, Instant.now());
    }

    public NotificationResponse retry(Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new NotificationNotFoundException(notificationId));

        if (notification.getStatus() == NotificationStatus.DELIVERED) {
            return toResponse(notification);
        }

        attemptEmailDelivery(notification);
        return toResponse(notification);
    }

    @Transactional(readOnly = true)
    public List<DeliveryRecordResponse> getDeliveryRecords(Long notificationId) {
        notificationRepository.findById(notificationId)
                .orElseThrow(() -> new NotificationNotFoundException(notificationId));

        return deliveryRecordRepository.findByNotificationIdOrderByAttemptNoAsc(notificationId).stream()
                .map(record -> new DeliveryRecordResponse(
                        record.getId(),
                        record.getChannel(),
                        record.getAttemptNo(),
                        record.getProvider(),
                        record.getStatus(),
                        record.getErrorMessage(),
                        record.getSentAt()
                ))
                .toList();
    }

    private NotificationResponse processNewNotification(EventNotificationRequest request) {
        NotificationTemplate inAppTemplate = templateService.getActiveTemplate(request.eventType(), DeliveryChannel.IN_APP);

        Notification notification = new Notification();
        notification.setEventKey(request.eventKey());
        notification.setEventType(request.eventType());
        notification.setRecipientUserId(request.recipientUserId());
        notification.setRecipientEmail(request.recipientEmail());
        notification.setTitle(renderingService.render(inAppTemplate.getSubjectTemplate(), request.templateData()));
        notification.setBody(renderingService.render(inAppTemplate.getBodyTemplate(), request.templateData()));
        notification.setStatus(NotificationStatus.CREATED);
        notification.setRead(false);
        notification.setCreatedAt(request.occurredAt() != null ? request.occurredAt() : Instant.now());

        var emailTemplate = templateService.findActiveTemplate(request.eventType(), DeliveryChannel.EMAIL);
        if (emailTemplate.isPresent()) {
            notification.setEmailSubject(renderingService.render(emailTemplate.get().getSubjectTemplate(), request.templateData()));
            notification.setEmailBody(renderingService.render(emailTemplate.get().getBodyTemplate(), request.templateData()));
        }

        notification = notificationRepository.save(notification);

        // In-app delivery is considered immediate once the notification is stored.
        createDeliveryRecord(notification, DeliveryChannel.IN_APP, DeliveryStatus.SENT, null, "in-app-store");
        notification.setStatus(NotificationStatus.DELIVERED);
        attemptEmailDelivery(notification);

        return toResponse(notification);
    }

    private void attemptEmailDelivery(Notification notification) {
        try {
            if (notification.getEmailSubject() == null || notification.getEmailBody() == null) {
                return;
            }

            emailSender.send(notification.getRecipientEmail(), notification.getEmailSubject(), notification.getEmailBody());
            createDeliveryRecord(notification, DeliveryChannel.EMAIL, DeliveryStatus.SENT, null, "logging-email");
            notification.setStatus(NotificationStatus.DELIVERED);
        } catch (Exception ex) {
            createDeliveryRecord(notification, DeliveryChannel.EMAIL, DeliveryStatus.FAILED, ex.getMessage(), "logging-email");
            notification.setStatus(NotificationStatus.PARTIALLY_DELIVERED);
        }
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

    private Notification getOwnedNotification(Long notificationId, Long recipientUserId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new NotificationNotFoundException(notificationId));
        if (!notification.getRecipientUserId().equals(recipientUserId)) {
            throw new NotificationNotFoundException(notificationId);
        }
        return notification;
    }

    private NotificationResponse toResponse(Notification notification) {
        return new NotificationResponse(
                notification.getId(),
                notification.getEventKey(),
                notification.getEventType(),
                notification.getRecipientUserId(),
                notification.getRecipientEmail(),
                notification.getTitle(),
                notification.getBody(),
                notification.getStatus(),
                notification.isRead(),
                notification.getCreatedAt(),
                notification.getReadAt()
        );
    }
}
