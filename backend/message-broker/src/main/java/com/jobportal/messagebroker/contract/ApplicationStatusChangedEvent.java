package com.jobportal.messagebroker.contract;

import java.time.Instant;

// Sent when an employer or the system moves an application to a new state.
public record ApplicationStatusChangedEvent(
        Long applicationId,
        String studentId,
        String employerId,
        Long jobId,
        String oldStatus,
        String newStatus,
        Instant timestamp
) {
}
