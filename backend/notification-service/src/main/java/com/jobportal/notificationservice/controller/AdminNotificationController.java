package com.jobportal.notificationservice.controller;

import com.jobportal.notificationservice.dto.ManualNotificationRequest;
import com.jobportal.notificationservice.dto.NotificationMetricsResponse;
import com.jobportal.notificationservice.dto.NotificationPageResponse;
import com.jobportal.notificationservice.dto.NotificationResponse;
import com.jobportal.notificationservice.service.NotificationAccessService;
import com.jobportal.notificationservice.service.NotificationQueryService;
import com.jobportal.notificationservice.service.NotificationWorkflowService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/notifications")
public class AdminNotificationController {

    private final NotificationAccessService notificationAccessService;
    private final NotificationQueryService notificationQueryService;
    private final NotificationWorkflowService notificationWorkflowService;

    public AdminNotificationController(
            NotificationAccessService notificationAccessService,
            NotificationQueryService notificationQueryService,
            NotificationWorkflowService notificationWorkflowService
    ) {
        this.notificationAccessService = notificationAccessService;
        this.notificationQueryService = notificationQueryService;
        this.notificationWorkflowService = notificationWorkflowService;
    }

    @GetMapping("/failed")
    public NotificationPageResponse getFailedNotifications(
            @RequestHeader(value = "X-User-Role", required = false) String userRole,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        notificationAccessService.requireAdminOrOperator(userRole);
        return notificationQueryService.getFailedNotifications(page, size);
    }

    @GetMapping("/metrics")
    public NotificationMetricsResponse getMetrics(
            @RequestHeader(value = "X-User-Role", required = false) String userRole
    ) {
        notificationAccessService.requireAdminOrOperator(userRole);
        return notificationQueryService.getMetrics();
    }

    @PostMapping("/{notificationId}/retry")
    public NotificationResponse retryNotification(
            @RequestHeader(value = "X-User-Role", required = false) String userRole,
            @PathVariable Long notificationId
    ) {
        notificationAccessService.requireAdminOrOperator(userRole);
        return notificationWorkflowService.retry(notificationId);
    }

    @PostMapping("/manual")
    public NotificationResponse sendManualNotification(
            @RequestHeader(value = "X-User-Role", required = false) String userRole,
            @Valid @RequestBody ManualNotificationRequest request
    ) {
        notificationAccessService.requireAdminOrOperator(userRole);
        return notificationWorkflowService.dispatchManualNotification(request);
    }

    @PostMapping("/retry-due")
    public Map<String, Integer> retryDueNotifications(
            @RequestHeader(value = "X-User-Role", required = false) String userRole
    ) {
        notificationAccessService.requireAdminOrOperator(userRole);
        return Map.of("processedCount", notificationWorkflowService.retryDueNotifications());
    }
}
