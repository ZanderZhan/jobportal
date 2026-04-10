package com.jobportal.notificationservice.repository;

import com.jobportal.notificationservice.entity.Notification;
import com.jobportal.notificationservice.entity.NotificationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    Optional<Notification> findByEventKey(String eventKey);

    List<Notification> findByRecipientUserIdOrderByCreatedAtDesc(Long recipientUserId);

    List<Notification> findByStatusOrderByCreatedAtDesc(NotificationStatus status);

    @Modifying
    @Query("UPDATE Notification n SET n.read = true, n.readAt = :readAt WHERE n.recipientUserId = :recipientUserId AND n.read = false")
    void markAllReadForRecipient(@Param("recipientUserId") Long recipientUserId, @Param("readAt") Instant readAt);
}
