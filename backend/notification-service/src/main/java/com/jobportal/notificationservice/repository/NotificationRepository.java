package com.jobportal.notificationservice.repository;

import com.jobportal.notificationservice.entity.Notification;
import com.jobportal.notificationservice.entity.NotificationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface NotificationRepository extends JpaRepository<Notification, Long>, JpaSpecificationExecutor<Notification> {

    Optional<Notification> findByEventKey(String eventKey);

    Optional<Notification> findFirstByRecipientUserIdOrderByCreatedAtDesc(String recipientUserId);

    Page<Notification> findByStatusOrderByCreatedAtDesc(NotificationStatus status, Pageable pageable);

    Page<Notification> findByStatusAndNextRetryAtBeforeOrderByNextRetryAtAsc(
            NotificationStatus status,
            Instant retryAt,
            Pageable pageable
    );

    long countByRecipientUserId(String recipientUserId);

    long countByRecipientUserIdAndReadFalse(String recipientUserId);

    long countByRecipientUserIdAndActionRequiredTrueAndReadFalse(String recipientUserId);

    long countByRecipientUserIdAndStatus(String recipientUserId, NotificationStatus status);

    long countByStatus(NotificationStatus status);

    @Modifying
    @Query("UPDATE Notification n SET n.read = true, n.readAt = :readAt WHERE n.recipientUserId = :recipientUserId AND n.read = false")
    void markAllReadForRecipient(@Param("recipientUserId") String recipientUserId, @Param("readAt") Instant readAt);
}
