package com.jobportal.jobservice.dto;

import com.jobportal.jobservice.entity.Job.EmploymentType;
import com.jobportal.jobservice.entity.Job.JobStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

public record JobRequest(
        @NotBlank(message = "Title is required")
        @Size(max = 255, message = "Title must be less than 255 characters")
        String title,

        @NotBlank(message = "Description is required")
        String description,

        @NotBlank(message = "Company is required")
        @Size(max = 255, message = "Company must be less than 255 characters")
        String company,

        @Size(max = 255, message = "Location must be less than 255 characters")
        String location,

        EmploymentType employmentType,

        @Positive(message = "Minimum salary must be positive")
        BigDecimal salaryMin,

        @Positive(message = "Maximum salary must be positive")
        BigDecimal salaryMax,

        @Size(max = 3, message = "Currency code must be 3 characters")
        String salaryCurrency,

        List<String> requirements,

        JobStatus status
) {}
