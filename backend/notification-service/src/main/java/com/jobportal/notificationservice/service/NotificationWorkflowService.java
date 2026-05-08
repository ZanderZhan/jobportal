package com.jobportal.notificationservice.service;

import com.jobportal.notificationservice.config.NotificationRetryProperties;
import com.jobportal.notificationservice.dto.EventNotificationRequest;
import com.jobportal.notificationservice.dto.ManualNotificationRequest;
import com.jobportal.notificationservice.dto.NotificationDispatchEvent;
import com.jobportal.notificationservice.dto.NotificationResponse;
import com.jobportal.notificationservice.dto.ResolvedRecipient;
import com.jobportal.notificationservice.entity.DeliveryChannel;
import com.jobportal.notificationservice.entity.Notification;
import com.jobportal.notificationservice.entity.NotificationStatus;
import com.jobportal.notificationservice.entity.NotificationTemplate;
import com.jobportal.notificationservice.exception.NotificationNotFoundException;
import com.jobportal.notificationservice.exception.NotificationRetryNotAllowedException;
import com.jobportal.notificationservice.repository.NotificationRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Service
@Transactional
public class NotificationWorkflowService {

    private final NotificationRepository notificationRepository;
    private final NotificationTemplateService templateService;
    private final TemplateRenderingService renderingService;
    private final NotificationPreferenceService preferenceService;
    private final RecipientIdentityService recipientIdentityService;
    private final NotificationDeliveryService notificationDeliveryService;
    private final NotificationAccessService notificationAccessService;
    private final NotificationMapper notificationMapper;
    private final NotificationActionPolicy notificationActionPolicy;
    private final NotificationRetryProperties retryProperties;
    private final NotificationDispatchPublisher notificationDispatchPublisher;

    public NotificationWorkflowService(
            NotificationRepository notificationRepository,
            NotificationTemplateService templateService,
            TemplateRenderingService renderingService,
            NotificationPreferenceService preferenceService,
            RecipientIdentityService recipientIdentityService,
            NotificationDeliveryService notificationDeliveryService,
            NotificationAccessService notificationAccessService,
            NotificationMapper notificationMapper,
            NotificationActionPolicy notificationActionPolicy,
            NotificationRetryProperties retryProperties,
            NotificationDispatchPublisher notificationDispatchPublisher
    ) {
        this.notificationRepository = notificationRepository;
        this.templateService = templateService;
        this.renderingService = renderingService;
        this.preferenceService = preferenceService;
        this.recipientIdentityService = recipientIdentityService;
        this.notificationDeliveryService = notificationDeliveryService;
        this.notificationAccessService = notificationAccessService;
        this.notificationMapper = notificationMapper;
        this.notificationActionPolicy = notificationActionPolicy;
        this.retryProperties = retryProperties;
        this.notificationDispatchPublisher = notificationDispatchPublisher;
    }

    public NotificationResponse handleEvent(EventNotificationRequest request) {
        return handleEventForDelivery(request).notification();
    }

    public NotificationProcessingResult handleEventForDelivery(EventNotificationRequest request) {
        return notificationRepository.findByEventKey(request.eventKey())
                .map(notification -> refreshExistingNotification(notification, request))
                .orElseGet(() -> processNewNotification(request));
    }

    public NotificationResponse markAsRead(Long notificationId, String userId, String role) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new NotificationNotFoundException(notificationId));

        notificationAccessService.ensureOwnerOrAdmin(notification, userId, role);

        notification.setRead(true);
        notification.setReadAt(Instant.now());
        return notificationMapper.toResponse(notification);
    }

    public void markAllAsRead(String recipientUserId) {
        notificationRepository.markAllReadForRecipient(recipientUserId, Instant.now());
    }

    public NotificationResponse retry(Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new NotificationNotFoundException(notificationId));

        if (notification.getStatus() == NotificationStatus.SENT) {
            throw new NotificationRetryNotAllowedException(notificationId, "already sent");
        }

        if (notification.getRetryCount() >= retryProperties.maxEmailAttempts()) {
            throw new NotificationRetryNotAllowedException(notificationId, "max retry attempts reached");
        }

        notification.setStatus(NotificationStatus.PENDING);
        notification.setNextRetryAt(null);
        notification.setRetryCount(0);
        notification.setFailureReason(null);
        refreshRecipientFromCache(notification);
        publishDispatch(notification);
        return notificationMapper.toResponse(notification);
    }

    public NotificationResponse dispatchManualNotification(ManualNotificationRequest request) {
        String eventKey = "manual-" + request.eventType().name().toLowerCase() + "-" + UUID.randomUUID();
        return handleEvent(new EventNotificationRequest(
                eventKey,
                request.eventType(),
                request.recipientUserId(),
                request.recipientEmail(),
                request.recipientName(),
                Instant.now(),
                request.templateData()
        ));
    }

    public void scheduleRecipientRecovery(Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new NotificationNotFoundException(notificationId));
        notificationDeliveryService.scheduleRecipientRecovery(notification);
    }

    public int retryDueNotifications() {
        int processedCount = 0;
        Instant now = Instant.now();
        int batchSize = Math.max(1, retryProperties.batchSize());
        Pageable retryBatch = PageRequest.of(0, batchSize, Sort.by(Sort.Direction.ASC, "nextRetryAt"));

        var dueEmailFailures = notificationRepository.findByStatusAndNextRetryAtBeforeOrderByNextRetryAtAsc(
                NotificationStatus.RETRYING,
                now,
                retryBatch
        );
        for (Notification notification : dueEmailFailures.getContent()) {
            notification.setStatus(NotificationStatus.PENDING);
            notification.setNextRetryAt(null);
            publishDispatch(notification);
            processedCount++;
        }

        var dueRecipientRecovery = notificationRepository.findByStatusAndNextRetryAtBeforeOrderByNextRetryAtAsc(
                NotificationStatus.PENDING,
                now,
                retryBatch
        );
        for (Notification notification : dueRecipientRecovery.getContent()) {
            refreshRecipientFromCache(notification);
            publishDispatch(notification);
            processedCount++;
        }

        return processedCount;
    }

    private NotificationProcessingResult processNewNotification(EventNotificationRequest request) {
        NotificationTemplate inAppTemplate = templateService.getActiveTemplate(request.eventType(), DeliveryChannel.IN_APP);
        ResolvedRecipient resolvedRecipient = resolveRecipient(request);
        Map<String, String> templateData = enrichTemplateData(request, resolvedRecipient);

        Notification notification = new Notification();
        notification.setEventKey(request.eventKey());
        notification.setEventType(request.eventType());
        notification.setRecipientUserId(resolvedRecipient.userId());
        notification.setRecipientEmail(resolvedRecipient.email());
        notification.setRecipientName(resolvedRecipient.name());
        notification.setTitle(renderingService.render(inAppTemplate.getSubjectTemplate(), templateData));
        notification.setBody(renderingService.render(inAppTemplate.getBodyTemplate(), templateData));
        notification.setActionRequired(notificationActionPolicy.isActionRequired(request.eventType(), templateData));
        notification.setStatus(NotificationStatus.PENDING);
        notification.setRead(false);
        notification.setCreatedAt(request.occurredAt() != null ? request.occurredAt() : Instant.now());

        var emailTemplate = templateService.findActiveTemplate(request.eventType(), DeliveryChannel.EMAIL);
        if (emailTemplate.isPresent()) {
            var template = emailTemplate.get();
            notification.setEmailSubject(renderingService.render(template.getSubjectTemplate(), templateData));
            notification.setEmailBody(renderingService.render(template.getBodyTemplate(), templateData));
        }

        notification = notificationRepository.save(notification);
        publishDispatch(notification);

        return new NotificationProcessingResult(
                notification.getId(),
                notificationMapper.toResponse(notification),
                false
        );
    }

    private NotificationProcessingResult refreshExistingNotification(Notification notification, EventNotificationRequest request) {
        ResolvedRecipient resolvedRecipient = resolveRecipient(request);
        applyRecipientDetails(notification, resolvedRecipient);

        if (notification.getStatus() != NotificationStatus.SENT) {
            publishDispatch(notification);
        }

        return new NotificationProcessingResult(
                notification.getId(),
                notificationMapper.toResponse(notification),
                false
        );
    }

    private void publishDispatch(Notification notification) {
        notificationDispatchPublisher.publish(new NotificationDispatchEvent(
                notification.getId(),
                notification.getRecipientUserId(),
                notification.getEventType(),
                notification.getCreatedAt()
        ));
    }

    private ResolvedRecipient resolveRecipient(EventNotificationRequest request) {
        if (StringUtils.hasText(request.recipientUserId())) {
            recipientIdentityService.remember(
                    request.recipientUserId(),
                    request.recipientEmail(),
                    request.recipientName(),
                    null,
                    "event-fallback"
            );
        }

        return recipientIdentityService.resolve(
                request.recipientUserId(),
                request.recipientEmail(),
                request.recipientName(),
                null
        );
    }

    private void refreshRecipientFromCache(Notification notification) {
        ResolvedRecipient resolvedRecipient = recipientIdentityService.resolve(
                notification.getRecipientUserId(),
                notification.getRecipientEmail(),
                notification.getRecipientName(),
                null
        );
        applyRecipientDetails(notification, resolvedRecipient);
    }

    private void applyRecipientDetails(Notification notification, ResolvedRecipient resolvedRecipient) {
        if (StringUtils.hasText(resolvedRecipient.email())) {
            notification.setRecipientEmail(resolvedRecipient.email());
        }
        if (StringUtils.hasText(resolvedRecipient.name())) {
            notification.setRecipientName(resolvedRecipient.name());
        }
    }

    private Map<String, String> enrichTemplateData(EventNotificationRequest request, ResolvedRecipient recipient) {
        Map<String, String> templateData = new LinkedHashMap<>();
        if (request.templateData() != null) {
            templateData.putAll(request.templateData());
        }

        if (recipient.userId() != null) {
            templateData.putIfAbsent("recipientUserId", recipient.userId());
        }
        if (recipient.name() != null) {
            templateData.putIfAbsent("recipientName", recipient.name());
        }
        if (recipient.email() != null) {
            templateData.putIfAbsent("recipientEmail", recipient.email());
        }

        return templateData;
    }
}
