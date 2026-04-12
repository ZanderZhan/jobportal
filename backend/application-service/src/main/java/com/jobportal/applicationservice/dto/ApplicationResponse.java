package com.jobportal.applicationservice.dto;

import com.jobportal.applicationservice.entity.Application;
import com.jobportal.applicationservice.entity.ApplicationTimelineEntry;
import com.jobportal.applicationservice.entity.ApplicationStatus;

import java.time.LocalDateTime;
import java.util.List;

public record ApplicationResponse(
        Long id,
        String studentId,
        Long jobId,
        String employerId,
        String jobTitle,
        String resumeReference,
        ApplicationStatus status,
        LocalDateTime submittedAt,
        LocalDateTime updatedAt,
        List<ApplicationTimelineEntryResponse> timeline
) {

    public static ApplicationResponse fromEntity(Application application) {
        return new ApplicationResponse(
                application.getId(),
                application.getStudentId(),
                application.getJobId(),
                application.getEmployerIdSnapshot(),
                application.getJobTitleSnapshot(),
                application.getResumeReference(),
                application.getStatus(),
                application.getSubmittedAt(),
                application.getUpdatedAt(),
                application.getTimelineEntries().stream()
                        .map(ApplicationTimelineEntryResponse::fromEntity)
                        .toList()
        );
    }
}
