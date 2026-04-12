package com.jobportal.profileservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record ExperienceEntryRequest(
        @NotBlank(message = "Company is required")
        @Size(max = 255, message = "Company must be less than 255 characters")
        String company,

        @NotBlank(message = "Title is required")
        @Size(max = 255, message = "Title must be less than 255 characters")
        String title,

        String description,

        LocalDate startDate,
        LocalDate endDate
) {}
