package com.jobportal.notificationservice.contract;

import java.time.Instant;

// This event is raised when a student withdraws an application.
public record ApplicationWithdrawnEvent(
        Long applicationId,
        String studentId,
        Long jobId,
        Instant timestamp
) {
}
