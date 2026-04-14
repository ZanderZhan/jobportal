package com.jobportal.notificationservice.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "notifications")
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // This keeps a stable key for deduplication and tracing.
    @Column(nullable = false, unique = true, length = 128)
    private String eventKey;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 64)
    private NotificationEventType eventType;

    @Column(nullable = false, length = 100)
    private String recipientUserId;

    @Column(length = 255)
    private String recipientEmail;

    @Column(length = 255)
    private String recipientName;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(nullable = false, length = 2000)
    private String body;

    @Column(nullable = false, columnDefinition = "boolean default false")
    private boolean actionRequired;

    // Email content is stored so failed deliveries can be retried later.
    @Column(length = 255)
    private String emailSubject;

    @Column(length = 2000)
    private String emailBody;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private NotificationStatus status;

    @Column(nullable = false)
    private boolean read;

    @Column(nullable = false)
    private Instant createdAt;

    private Instant readAt;

    @Column(length = 1000)
    private String lastDeliveryError;

    private Instant lastDeliveryAttemptAt;

    private Instant nextRetryAt;

    @Column(nullable = false)
    private Integer emailAttemptCount = 0;

    @OneToMany(mappedBy = "notification", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<DeliveryRecord> deliveryRecords = new ArrayList<>();

    public void addDeliveryRecord(DeliveryRecord deliveryRecord) {
        deliveryRecords.add(deliveryRecord);
        deliveryRecord.setNotification(this);
    }
}
