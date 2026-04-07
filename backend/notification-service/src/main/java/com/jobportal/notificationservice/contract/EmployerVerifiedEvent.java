package com.jobportal.notificationservice.contract;

import java.time.Instant;

// This event is raised when an employer becomes verified.
public record EmployerVerifiedEvent(
        Long employerId,
        String verificationStatus,
        Instant timestamp
) {
}
