package com.jobportal.messagebroker.contract;

import java.time.Instant;

// Sent after a student application is created successfully.
public record ApplicationSubmittedEvent(
        Long applicationId,
        String studentId,
        Long jobId,
        Instant timestamp
) {
}
