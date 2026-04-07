package com.jobportal.notificationservice.controller;

import com.jobportal.notificationservice.dto.CreateTemplateRequest;
import com.jobportal.notificationservice.dto.NotificationTemplateResponse;
import com.jobportal.notificationservice.service.NotificationTemplateService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/notification-templates")
public class NotificationTemplateController {

    private final NotificationTemplateService templateService;

    public NotificationTemplateController(NotificationTemplateService templateService) {
        this.templateService = templateService;
    }

    @GetMapping
    public List<NotificationTemplateResponse> getTemplates() {
        return templateService.getActiveTemplates();
    }

    @PostMapping
    public NotificationTemplateResponse createTemplate(@Valid @RequestBody CreateTemplateRequest request) {
        return templateService.createTemplate(request);
    }
}
