package com.jobportal.applicationservice.dto;

import com.jobportal.applicationservice.entity.ApplicationStatus;
import com.jobportal.applicationservice.entity.ApplicationTimelineEntry;

import java.time.LocalDateTime;

public record ApplicationTimelineEntryResponse(
        Long id,
        ApplicationStatus oldStatus,
        ApplicationStatus newStatus,
        String changedBy,
        String reason,
        LocalDateTime createdAt
) {

    public static ApplicationTimelineEntryResponse fromEntity(ApplicationTimelineEntry entry) {
        return new ApplicationTimelineEntryResponse(
                entry.getId(),
                entry.getOldStatus(),
                entry.getNewStatus(),
                entry.getChangedBy(),
                entry.getReason(),
                entry.getCreatedAt()
        );
    }
}
