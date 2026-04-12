package com.jobportal.applicationservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record ApplicationCreateRequest(
        @NotNull(message = "Job ID is required")
        @Positive(message = "Job ID must be positive")
        Long jobId,

        @NotBlank(message = "Resume reference is required")
        @Size(max = 500, message = "Resume reference must be less than 500 characters")
        String resumeReference
) {}
