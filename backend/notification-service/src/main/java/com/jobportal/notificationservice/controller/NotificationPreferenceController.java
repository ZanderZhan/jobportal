package com.jobportal.notificationservice.controller;

import com.jobportal.notificationservice.dto.NotificationPreferenceResponse;
import com.jobportal.notificationservice.dto.UpdateNotificationPreferenceRequest;
import com.jobportal.notificationservice.entity.NotificationEventType;
import com.jobportal.notificationservice.service.NotificationAccessService;
import com.jobportal.notificationservice.service.NotificationPreferenceService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/notification-preferences")
public class NotificationPreferenceController {

    private final NotificationPreferenceService notificationPreferenceService;
    private final NotificationAccessService notificationAccessService;

    public NotificationPreferenceController(
            NotificationPreferenceService notificationPreferenceService,
            NotificationAccessService notificationAccessService
    ) {
        this.notificationPreferenceService = notificationPreferenceService;
        this.notificationAccessService = notificationAccessService;
    }

    @GetMapping("/me")
    public List<NotificationPreferenceResponse> getMyPreferences(
            @RequestHeader(value = "X-User-Id", required = false) String recipientUserId
    ) {
        return notificationPreferenceService.getPreferencesForUser(
                notificationAccessService.requireUserId(recipientUserId)
        );
    }

    @PutMapping("/me/{eventType}")
    public NotificationPreferenceResponse updateMyPreference(
            @RequestHeader(value = "X-User-Id", required = false) String recipientUserId,
            @PathVariable NotificationEventType eventType,
            @Valid @RequestBody UpdateNotificationPreferenceRequest request
    ) {
        return notificationPreferenceService.updatePreference(
                notificationAccessService.requireUserId(recipientUserId),
                eventType,
                request
        );
    }
}
