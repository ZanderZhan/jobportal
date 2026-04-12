package com.jobportal.applicationservice.event;

import java.time.Instant;

public record ApplicationStatusUpdatedEvent(
        Long applicationId,
        String studentId,
        String employerId,
        Long jobId,
        String oldStatus,
        String newStatus,
        Instant timestamp
) {
}
