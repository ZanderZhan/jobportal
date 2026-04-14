package com.jobportal.notificationservice.contract;

import java.time.Instant;

// This event is raised when an employer becomes verified.
public record EmployerVerifiedEvent(
        String employerId,
        String verificationStatus,
        Instant timestamp
) {
}
