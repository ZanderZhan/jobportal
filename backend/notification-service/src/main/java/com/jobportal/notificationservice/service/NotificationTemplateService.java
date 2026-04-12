package com.jobportal.notificationservice.service;

import com.jobportal.notificationservice.dto.CreateTemplateRequest;
import com.jobportal.notificationservice.dto.NotificationTemplateResponse;
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

    public NotificationTemplateService(NotificationTemplateRepository templateRepository) {
        this.templateRepository = templateRepository;
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
                .map(this::toResponse)
                .toList();
    }

    public NotificationTemplateResponse createTemplate(CreateTemplateRequest request) {
        NotificationTemplate template = new NotificationTemplate();
        template.setEventType(request.eventType());
        template.setChannel(request.channel());
        template.setSubjectTemplate(request.subjectTemplate());
        template.setBodyTemplate(request.bodyTemplate());
        template.setActive(true);
        return toResponse(templateRepository.save(template));
    }

    private NotificationTemplateResponse toResponse(NotificationTemplate template) {
        return new NotificationTemplateResponse(
                template.getId(),
                template.getEventType(),
                template.getChannel(),
                template.getSubjectTemplate(),
                template.getBodyTemplate(),
                template.isActive()
        );
    }
}
