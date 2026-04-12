package com.jobportal.notificationservice.contract;

import java.time.Instant;

// This event is raised when an application moves to a new status.
public record ApplicationStatusChangedEvent(
        Long applicationId,
        String oldStatus,
        String newStatus,
        Instant timestamp
) {
}
