package com.jobportal.notificationservice.repository;

import com.jobportal.notificationservice.entity.DeliveryChannel;
import com.jobportal.notificationservice.entity.NotificationEventType;
import com.jobportal.notificationservice.entity.NotificationTemplate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface NotificationTemplateRepository extends JpaRepository<NotificationTemplate, Long> {

    Optional<NotificationTemplate> findByEventTypeAndChannelAndActiveTrue(
            NotificationEventType eventType,
            DeliveryChannel channel
    );

    List<NotificationTemplate> findByActiveTrueOrderByEventTypeAscChannelAsc();
}
