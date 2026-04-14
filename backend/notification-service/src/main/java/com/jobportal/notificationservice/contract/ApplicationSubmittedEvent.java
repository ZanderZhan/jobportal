package com.jobportal.notificationservice.contract;

import java.time.Instant;

// This event is raised when a student submits an application.
public record ApplicationSubmittedEvent(
        Long applicationId,
        String studentId,
        Long jobId,
        Instant timestamp
) {
}
