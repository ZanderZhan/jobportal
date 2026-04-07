package com.jobportal.notificationservice.controller;

import com.jobportal.notificationservice.dto.DeliveryRecordResponse;
import com.jobportal.notificationservice.dto.NotificationResponse;
import com.jobportal.notificationservice.service.NotificationWorkflowService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationWorkflowService notificationWorkflowService;

    public NotificationController(NotificationWorkflowService notificationWorkflowService) {
        this.notificationWorkflowService = notificationWorkflowService;
    }

    @GetMapping("/me")
    public List<NotificationResponse> getMyNotifications(@RequestParam Long recipientUserId) {
        return notificationWorkflowService.getNotificationsForRecipient(recipientUserId);
    }

    @PatchMapping("/{notificationId}/read")
    public NotificationResponse markAsRead(
            @PathVariable Long notificationId,
            @RequestParam Long recipientUserId
    ) {
        return notificationWorkflowService.markAsRead(notificationId, recipientUserId);
    }

    @PatchMapping("/read-all")
    public void markAllAsRead(@RequestParam Long recipientUserId) {
        notificationWorkflowService.markAllAsRead(recipientUserId);
    }

    @GetMapping("/failed")
    public List<NotificationResponse> getFailedNotifications() {
        return notificationWorkflowService.getFailedNotifications();
    }

    @PostMapping("/{notificationId}/retry")
    public NotificationResponse retryFailedNotification(@PathVariable Long notificationId) {
        return notificationWorkflowService.retry(notificationId);
    }

    @GetMapping("/{notificationId}/deliveries")
    public List<DeliveryRecordResponse> getDeliveryRecords(@PathVariable Long notificationId) {
        return notificationWorkflowService.getDeliveryRecords(notificationId);
    }
}
