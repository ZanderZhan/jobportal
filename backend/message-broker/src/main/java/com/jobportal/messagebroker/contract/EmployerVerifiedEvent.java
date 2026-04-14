package com.jobportal.messagebroker.contract;

import java.time.Instant;

// Sent when an employer account becomes verified and can post jobs.
public record EmployerVerifiedEvent(
        Long employerId,
        String verificationStatus,
        Instant timestamp
) {
}
