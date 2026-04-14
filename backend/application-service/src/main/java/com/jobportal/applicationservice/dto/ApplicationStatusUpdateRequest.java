package com.jobportal.applicationservice.dto;

import com.jobportal.applicationservice.entity.ApplicationStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ApplicationStatusUpdateRequest(
        @NotNull(message = "Status is required")
        ApplicationStatus status,

        @Size(max = 255, message = "Reason must be less than 255 characters")
        String reason
) {}
