package com.jobportal.applicationservice.event;

import java.time.Instant;

public record ApplicationWithdrawnEvent(
        Long applicationId,
        String studentId,
        Long jobId,
        Instant timestamp
) {
}
