package com.jobportal.notificationservice.service;

import com.jobportal.notificationservice.dto.CreateTemplateRequest;
import com.jobportal.notificationservice.dto.NotificationTemplateResponse;
import com.jobportal.notificationservice.dto.TemplatePreviewRequest;
import com.jobportal.notificationservice.dto.TemplatePreviewResponse;
import com.jobportal.notificationservice.entity.DeliveryChannel;
import com.jobportal.notificationservice.entity.NotificationEventType;
import com.jobportal.notificationservice.entity.NotificationTemplate;
import com.jobportal.notificationservice.exception.TemplateNotFoundException;
import com.jobportal.notificationservice.repository.NotificationTemplateRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class NotificationTemplateService {

    private final NotificationTemplateRepository templateRepository;
    private final TemplateRenderingService templateRenderingService;
    private final NotificationMapper notificationMapper;

    public NotificationTemplateService(
            NotificationTemplateRepository templateRepository,
            TemplateRenderingService templateRenderingService,
            NotificationMapper notificationMapper
    ) {
        this.templateRepository = templateRepository;
        this.templateRenderingService = templateRenderingService;
        this.notificationMapper = notificationMapper;
    }

    @Transactional(readOnly = true)
    public NotificationTemplate getActiveTemplate(NotificationEventType eventType, DeliveryChannel channel) {
        return templateRepository.findByEventTypeAndChannelAndActiveTrue(eventType, channel)
                .orElseThrow(() -> new TemplateNotFoundException(eventType, channel));
    }

    @Transactional(readOnly = true)
    public Optional<NotificationTemplate> findActiveTemplate(NotificationEventType eventType, DeliveryChannel channel) {
        return templateRepository.findByEventTypeAndChannelAndActiveTrue(eventType, channel);
    }

    @Transactional(readOnly = true)
    public List<NotificationTemplateResponse> getActiveTemplates() {
        return templateRepository.findByActiveTrueOrderByEventTypeAscChannelAsc().stream()
                .map(notificationMapper::toTemplateResponse)
                .toList();
    }

    public NotificationTemplateResponse createTemplate(CreateTemplateRequest request) {
        templateRepository.findByEventTypeAndChannelAndActiveTrue(request.eventType(), request.channel())
                .ifPresent(existingTemplate -> existingTemplate.setActive(false));

        NotificationTemplate template = new NotificationTemplate();
        template.setEventType(request.eventType());
        template.setChannel(request.channel());
        template.setSubjectTemplate(request.subjectTemplate());
        template.setBodyTemplate(request.bodyTemplate());
        template.setActive(true);
        return notificationMapper.toTemplateResponse(templateRepository.save(template));
    }

    @Transactional(readOnly = true)
    public TemplatePreviewResponse previewTemplate(TemplatePreviewRequest request) {
        NotificationTemplate template = getActiveTemplate(request.eventType(), request.channel());
        return new TemplatePreviewResponse(
                request.eventType(),
                request.channel(),
                templateRenderingService.render(template.getSubjectTemplate(), request.templateData()),
                templateRenderingService.render(template.getBodyTemplate(), request.templateData())
        );
    }

    @Transactional(readOnly = true)
    public long countActiveTemplates() {
        return templateRepository.countByActiveTrue();
    }
}
