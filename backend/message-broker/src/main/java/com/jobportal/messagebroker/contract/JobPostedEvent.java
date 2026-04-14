package com.jobportal.messagebroker.contract;

import java.time.Instant;

// Sent after a job post is published and ready for downstream actions.
public record JobPostedEvent(
        Long jobId,
        Long employerId,
        String title,
        Instant timestamp
) {
}
