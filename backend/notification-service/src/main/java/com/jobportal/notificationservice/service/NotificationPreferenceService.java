package com.jobportal.notificationservice.service;

import com.jobportal.notificationservice.dto.NotificationPreferenceResponse;
import com.jobportal.notificationservice.dto.UpdateNotificationPreferenceRequest;
import com.jobportal.notificationservice.entity.NotificationEventType;
import com.jobportal.notificationservice.entity.NotificationPreference;
import com.jobportal.notificationservice.repository.NotificationPreferenceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;

@Service
@Transactional
public class NotificationPreferenceService {

    private final NotificationPreferenceRepository preferenceRepository;
    private final NotificationMapper notificationMapper;

    public NotificationPreferenceService(
            NotificationPreferenceRepository preferenceRepository,
            NotificationMapper notificationMapper
    ) {
        this.preferenceRepository = preferenceRepository;
        this.notificationMapper = notificationMapper;
    }

    @Transactional(readOnly = true)
    public List<NotificationPreferenceResponse> getPreferencesForUser(String recipientUserId) {
        return Arrays.stream(NotificationEventType.values())
                .map(eventType -> notificationMapper.toPreferenceResponse(resolvePreference(recipientUserId, eventType)))
                .toList();
    }

    @Transactional(readOnly = true)
    public NotificationPreference resolvePreference(String recipientUserId, NotificationEventType eventType) {
        return preferenceRepository.findByRecipientUserIdAndEventType(recipientUserId, eventType)
                .orElseGet(() -> defaultPreference(recipientUserId, eventType));
    }

    public NotificationPreferenceResponse updatePreference(
            String recipientUserId,
            NotificationEventType eventType,
            UpdateNotificationPreferenceRequest request
    ) {
        NotificationPreference preference = preferenceRepository.findByRecipientUserIdAndEventType(recipientUserId, eventType)
                .orElseGet(() -> {
                    NotificationPreference created = new NotificationPreference();
                    created.setRecipientUserId(recipientUserId);
                    created.setEventType(eventType);
                    return created;
                });

        preference.setInAppEnabled(request.inAppEnabled());
        preference.setEmailEnabled(request.emailEnabled());

        return notificationMapper.toPreferenceResponse(preferenceRepository.save(preference));
    }

    @Transactional(readOnly = true)
    public long countStoredPreferences() {
        return preferenceRepository.count();
    }

    private NotificationPreference defaultPreference(String recipientUserId, NotificationEventType eventType) {
        NotificationPreference preference = new NotificationPreference();
        preference.setRecipientUserId(recipientUserId);
        preference.setEventType(eventType);
        preference.setInAppEnabled(true);
        preference.setEmailEnabled(true);
        return preference;
    }
}
