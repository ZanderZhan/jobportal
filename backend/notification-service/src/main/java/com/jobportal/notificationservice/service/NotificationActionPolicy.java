package com.jobportal.notificationservice.service;

import com.jobportal.notificationservice.entity.NotificationEventType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Component
public class NotificationActionPolicy {

    private static final Set<String> ACTION_STATUS = Set.of("INTERVIEW", "HIRED", "REJECTED", "OFFERED");

    public boolean isActionRequired(NotificationEventType eventType, Map<String, String> templateData) {
        if (eventType == NotificationEventType.APPLICATION_STATUS_CHANGED) {
            String newStatus = normalize(templateData.get("newStatus"));
            return ACTION_STATUS.contains(newStatus);
        }

        return eventType == NotificationEventType.EMPLOYER_VERIFIED;
    }

    private String normalize(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        return value.trim().replace(' ', '_').toUpperCase(Locale.ROOT);
    }
}
