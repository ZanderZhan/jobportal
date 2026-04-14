package com.jobportal.notificationservice.service;

import com.jobportal.notificationservice.dto.DeliveryRecordResponse;
import com.jobportal.notificationservice.dto.NotificationBootstrapResponse;
import com.jobportal.notificationservice.dto.NotificationMetricsResponse;
import com.jobportal.notificationservice.dto.NotificationPageResponse;
import com.jobportal.notificationservice.dto.NotificationResponse;
import com.jobportal.notificationservice.dto.NotificationSummaryResponse;
import com.jobportal.notificationservice.entity.Notification;
import com.jobportal.notificationservice.entity.NotificationEventType;
import com.jobportal.notificationservice.entity.NotificationStatus;
import com.jobportal.notificationservice.exception.NotificationNotFoundException;
import com.jobportal.notificationservice.repository.DeliveryRecordRepository;
import com.jobportal.notificationservice.repository.NotificationRepository;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class NotificationQueryService {

    private final NotificationRepository notificationRepository;
    private final DeliveryRecordRepository deliveryRecordRepository;
    private final NotificationMapper notificationMapper;
    private final NotificationAccessService notificationAccessService;
    private final NotificationPreferenceService notificationPreferenceService;
    private final NotificationTemplateService notificationTemplateService;
    private final RecipientIdentityService recipientIdentityService;

    public NotificationQueryService(
            NotificationRepository notificationRepository,
            DeliveryRecordRepository deliveryRecordRepository,
            NotificationMapper notificationMapper,
            NotificationAccessService notificationAccessService,
            NotificationPreferenceService notificationPreferenceService,
            NotificationTemplateService notificationTemplateService,
            RecipientIdentityService recipientIdentityService
    ) {
        this.notificationRepository = notificationRepository;
        this.deliveryRecordRepository = deliveryRecordRepository;
        this.notificationMapper = notificationMapper;
        this.notificationAccessService = notificationAccessService;
        this.notificationPreferenceService = notificationPreferenceService;
        this.notificationTemplateService = notificationTemplateService;
        this.recipientIdentityService = recipientIdentityService;
    }

    private static final int MAX_PAGE_SIZE = 100;

    public NotificationPageResponse getNotificationsForUser(
            String recipientUserId,
            NotificationStatus status,
            NotificationEventType eventType,
            boolean unreadOnly,
            boolean actionRequiredOnly,
            int page,
            int size
    ) {
        Pageable pageable = PageRequest.of(page, Math.min(size, MAX_PAGE_SIZE), Sort.by(Sort.Direction.DESC, "createdAt"));

        Specification<Notification> specification = (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(criteriaBuilder.equal(root.get("recipientUserId"), recipientUserId));

            if (status != null) {
                predicates.add(criteriaBuilder.equal(root.get("status"), status));
            }

            if (eventType != null) {
                predicates.add(criteriaBuilder.equal(root.get("eventType"), eventType));
            }

            if (unreadOnly) {
                predicates.add(criteriaBuilder.isFalse(root.get("read")));
            }

            if (actionRequiredOnly) {
                predicates.add(criteriaBuilder.isTrue(root.get("actionRequired")));
            }

            return criteriaBuilder.and(predicates.toArray(Predicate[]::new));
        };

        Page<Notification> notifications = notificationRepository.findAll(specification, pageable);

        return new NotificationPageResponse(
                notifications.getContent().stream().map(notificationMapper::toResponse).toList(),
                notifications.getNumber(),
                notifications.getSize(),
                notifications.getTotalElements(),
                notifications.getTotalPages()
        );
    }

    public NotificationSummaryResponse getSummaryForUser(String recipientUserId) {
        Instant latestNotificationAt = notificationRepository.findFirstByRecipientUserIdOrderByCreatedAtDesc(recipientUserId)
                .map(Notification::getCreatedAt)
                .orElse(null);

        return new NotificationSummaryResponse(
                notificationRepository.countByRecipientUserId(recipientUserId),
                notificationRepository.countByRecipientUserIdAndReadFalse(recipientUserId),
                notificationRepository.countByRecipientUserIdAndActionRequiredTrueAndReadFalse(recipientUserId),
                notificationRepository.countByRecipientUserIdAndStatus(recipientUserId, NotificationStatus.FAILED),
                notificationRepository.countByRecipientUserIdAndStatus(recipientUserId, NotificationStatus.PENDING_RECIPIENT),
                notificationRepository.countByRecipientUserIdAndStatus(recipientUserId, NotificationStatus.RETRY_SCHEDULED),
                latestNotificationAt
        );
    }

    public NotificationBootstrapResponse getBootstrapForUser(String recipientUserId) {
        var recipient = recipientIdentityService.resolve(recipientUserId, null, null, null);
        return new NotificationBootstrapResponse(
                recipientUserId,
                recipient.email(),
                recipient.name(),
                StringUtils.hasText(recipient.email()),
                getSummaryForUser(recipientUserId)
        );
    }

    public NotificationPageResponse getFailedNotifications(int page, int size) {
        Pageable pageable = PageRequest.of(page, Math.min(size, MAX_PAGE_SIZE), Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Notification> failedNotifications = notificationRepository.findByStatusOrderByCreatedAtDesc(NotificationStatus.FAILED, pageable);

        return new NotificationPageResponse(
                failedNotifications.getContent().stream().map(notificationMapper::toResponse).toList(),
                failedNotifications.getNumber(),
                failedNotifications.getSize(),
                failedNotifications.getTotalElements(),
                failedNotifications.getTotalPages()
        );
    }

    public List<DeliveryRecordResponse> getDeliveryRecords(Long notificationId, String userId, String role) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new NotificationNotFoundException(notificationId));

        notificationAccessService.ensureOwnerOrAdmin(notification, userId, role);

        return deliveryRecordRepository.findByNotificationIdOrderByAttemptNoAsc(notificationId).stream()
                .map(notificationMapper::toDeliveryResponse)
                .toList();
    }

    public NotificationMetricsResponse getMetrics() {
        return new NotificationMetricsResponse(
                notificationRepository.count(),
                notificationRepository.countByStatus(NotificationStatus.DELIVERED),
                notificationRepository.countByStatus(NotificationStatus.FAILED),
                notificationRepository.countByStatus(NotificationStatus.RETRY_SCHEDULED),
                notificationRepository.countByStatus(NotificationStatus.SUPPRESSED),
                notificationPreferenceService.countStoredPreferences(),
                notificationTemplateService.countActiveTemplates()
        );
    }
}
