package com.jobportal.messagebroker.contract;

import java.time.Instant;

// Sent after a student withdraws an application successfully.
public record ApplicationWithdrawnEvent(
        Long applicationId,
        String studentId,
        Long jobId,
        Instant timestamp
) {
}
