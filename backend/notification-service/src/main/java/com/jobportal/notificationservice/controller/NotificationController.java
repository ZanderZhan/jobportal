package com.jobportal.notificationservice.controller;

import com.jobportal.notificationservice.dto.DeliveryRecordResponse;
import com.jobportal.notificationservice.dto.NotificationBootstrapResponse;
import com.jobportal.notificationservice.dto.NotificationPageResponse;
import com.jobportal.notificationservice.dto.NotificationResponse;
import com.jobportal.notificationservice.dto.NotificationSummaryResponse;
import com.jobportal.notificationservice.entity.NotificationEventType;
import com.jobportal.notificationservice.entity.NotificationStatus;
import com.jobportal.notificationservice.service.NotificationAccessService;
import com.jobportal.notificationservice.service.NotificationQueryService;
import com.jobportal.notificationservice.service.NotificationWorkflowService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationWorkflowService notificationWorkflowService;
    private final NotificationQueryService notificationQueryService;
    private final NotificationAccessService notificationAccessService;

    public NotificationController(
            NotificationWorkflowService notificationWorkflowService,
            NotificationQueryService notificationQueryService,
            NotificationAccessService notificationAccessService
    ) {
        this.notificationWorkflowService = notificationWorkflowService;
        this.notificationQueryService = notificationQueryService;
        this.notificationAccessService = notificationAccessService;
    }

    @GetMapping("/bootstrap")
    public NotificationBootstrapResponse bootstrapRecipient(
            @RequestHeader(value = "X-User-Id", required = false) String recipientUserId
    ) {
        return notificationQueryService.getBootstrapForUser(
                notificationAccessService.requireUserId(recipientUserId)
        );
    }

    @GetMapping("/me")
    public NotificationPageResponse getMyNotifications(
            @RequestHeader(value = "X-User-Id", required = false) String recipientUserId,
            @RequestParam(required = false) NotificationStatus status,
            @RequestParam(required = false) NotificationEventType eventType,
            @RequestParam(defaultValue = "false") boolean unreadOnly,
            @RequestParam(defaultValue = "false") boolean actionRequiredOnly,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return notificationQueryService.getNotificationsForUser(
                notificationAccessService.requireUserId(recipientUserId),
                status,
                eventType,
                unreadOnly,
                actionRequiredOnly,
                page,
                size
        );
    }

    @GetMapping("/summary")
    public NotificationSummaryResponse getMySummary(
            @RequestHeader(value = "X-User-Id", required = false) String recipientUserId
    ) {
        return notificationQueryService.getSummaryForUser(notificationAccessService.requireUserId(recipientUserId));
    }

    @PatchMapping("/{notificationId}/read")
    public NotificationResponse markAsRead(
            @PathVariable Long notificationId,
            @RequestHeader(value = "X-User-Id", required = false) String recipientUserId,
            @RequestHeader(value = "X-User-Role", required = false) String userRole
    ) {
        return notificationWorkflowService.markAsRead(
                notificationId,
                notificationAccessService.requireUserId(recipientUserId),
                notificationAccessService.requireRole(userRole)
        );
    }

    @PatchMapping("/read-all")
    public void markAllAsRead(@RequestHeader(value = "X-User-Id", required = false) String recipientUserId) {
        notificationWorkflowService.markAllAsRead(notificationAccessService.requireUserId(recipientUserId));
    }

    @GetMapping("/{notificationId}/deliveries")
    public List<DeliveryRecordResponse> getDeliveryRecords(
            @PathVariable Long notificationId,
            @RequestHeader(value = "X-User-Id", required = false) String recipientUserId,
            @RequestHeader(value = "X-User-Role", required = false) String userRole
    ) {
        return notificationQueryService.getDeliveryRecords(
                notificationId,
                recipientUserId,
                notificationAccessService.requireRole(userRole)
        );
    }
}
