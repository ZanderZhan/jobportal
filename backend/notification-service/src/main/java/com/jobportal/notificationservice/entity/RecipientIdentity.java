package com.jobportal.notificationservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "recipient_identity")
public class RecipientIdentity {

    @Id
    @Column(nullable = false, length = 100)
    private String userId;

    @Column(length = 255)
    private String email;

    @Column(length = 255)
    private String displayName;

    @Column(length = 64)
    private String role;

    @Column(length = 64)
    private String source;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    @Column(nullable = false)
    private Instant lastSeenAt;
}
