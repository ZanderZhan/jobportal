package com.jobportal.notificationservice.service;

import com.jobportal.notificationservice.dto.DeliveryRecordResponse;
import com.jobportal.notificationservice.dto.NotificationPreferenceResponse;
import com.jobportal.notificationservice.dto.NotificationResponse;
import com.jobportal.notificationservice.dto.NotificationTemplateResponse;
import com.jobportal.notificationservice.entity.DeliveryRecord;
import com.jobportal.notificationservice.entity.Notification;
import com.jobportal.notificationservice.entity.NotificationPreference;
import com.jobportal.notificationservice.entity.NotificationTemplate;
import org.springframework.stereotype.Component;

@Component
public class NotificationMapper {

    public NotificationResponse toResponse(Notification notification) {
        return new NotificationResponse(
                notification.getId(),
                notification.getEventKey(),
                notification.getEventType(),
                notification.getRecipientUserId(),
                notification.getRecipientEmail(),
                notification.getRecipientName(),
                notification.getTitle(),
                notification.getBody(),
                notification.isActionRequired(),
                notification.getStatus(),
                notification.isRead(),
                notification.getCreatedAt(),
                notification.getReadAt(),
                notification.getLastDeliveryError(),
                notification.getNextRetryAt()
        );
    }

    public DeliveryRecordResponse toDeliveryResponse(DeliveryRecord record) {
        return new DeliveryRecordResponse(
                record.getId(),
                record.getChannel(),
                record.getAttemptNo(),
                record.getProvider(),
                record.getStatus(),
                record.getErrorMessage(),
                record.getSentAt()
        );
    }

    public NotificationTemplateResponse toTemplateResponse(NotificationTemplate template) {
        return new NotificationTemplateResponse(
                template.getId(),
                template.getEventType(),
                template.getChannel(),
                template.getSubjectTemplate(),
                template.getBodyTemplate(),
                template.isActive()
        );
    }

    public NotificationPreferenceResponse toPreferenceResponse(NotificationPreference preference) {
        return new NotificationPreferenceResponse(
                preference.getEventType(),
                preference.isInAppEnabled(),
                preference.isEmailEnabled()
        );
    }
}
