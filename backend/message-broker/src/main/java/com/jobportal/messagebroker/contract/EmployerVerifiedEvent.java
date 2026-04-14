package com.jobportal.messagebroker.contract;

import java.time.Instant;

// Sent when an employer account becomes verified and can post jobs.
public record EmployerVerifiedEvent(
        String employerId,
        String verificationStatus,
        Instant timestamp
) {
}
