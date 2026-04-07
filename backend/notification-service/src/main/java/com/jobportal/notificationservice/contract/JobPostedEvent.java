package com.jobportal.notificationservice.contract;

import java.time.Instant;

// This event is raised when a job is published.
public record JobPostedEvent(
        Long jobId,
        Long employerId,
        String title,
        Instant timestamp
) {
}
