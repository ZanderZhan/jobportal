package com.jobportal.notificationservice.controller;

import com.jobportal.notificationservice.dto.CreateTemplateRequest;
import com.jobportal.notificationservice.dto.NotificationTemplateResponse;
import com.jobportal.notificationservice.dto.TemplatePreviewRequest;
import com.jobportal.notificationservice.dto.TemplatePreviewResponse;
import com.jobportal.notificationservice.service.NotificationAccessService;
import com.jobportal.notificationservice.service.NotificationTemplateService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/notification-templates")
public class NotificationTemplateController {

    private final NotificationTemplateService templateService;
    private final NotificationAccessService notificationAccessService;

    public NotificationTemplateController(
            NotificationTemplateService templateService,
            NotificationAccessService notificationAccessService
    ) {
        this.templateService = templateService;
        this.notificationAccessService = notificationAccessService;
    }

    @GetMapping
    public List<NotificationTemplateResponse> getTemplates(
            @RequestHeader(value = "X-User-Role", required = false) String userRole
    ) {
        notificationAccessService.requireAdmin(userRole);
        return templateService.getActiveTemplates();
    }

    @PostMapping
    public NotificationTemplateResponse createTemplate(
            @RequestHeader(value = "X-User-Role", required = false) String userRole,
            @Valid @RequestBody CreateTemplateRequest request
    ) {
        notificationAccessService.requireAdmin(userRole);
        return templateService.createTemplate(request);
    }

    @PostMapping("/preview")
    public TemplatePreviewResponse previewTemplate(
            @RequestHeader(value = "X-User-Role", required = false) String userRole,
            @Valid @RequestBody TemplatePreviewRequest request
    ) {
        notificationAccessService.requireAdmin(userRole);
        return templateService.previewTemplate(request);
    }
}
