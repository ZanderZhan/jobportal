package com.jobportal.notificationservice.repository;

import com.jobportal.notificationservice.entity.NotificationEventType;
import com.jobportal.notificationservice.entity.NotificationPreference;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface NotificationPreferenceRepository extends JpaRepository<NotificationPreference, Long> {

    List<NotificationPreference> findByRecipientUserIdOrderByEventTypeAsc(String recipientUserId);

    Optional<NotificationPreference> findByRecipientUserIdAndEventType(String recipientUserId, NotificationEventType eventType);
}
