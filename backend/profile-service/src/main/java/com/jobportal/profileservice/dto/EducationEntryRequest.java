package com.jobportal.profileservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record EducationEntryRequest(
        @NotBlank(message = "Institution is required")
        @Size(max = 255, message = "Institution must be less than 255 characters")
        String institution,

        @Size(max = 255, message = "Degree must be less than 255 characters")
        String degree,

        @Size(max = 255, message = "Field of study must be less than 255 characters")
        String fieldOfStudy,

        LocalDate startDate,
        LocalDate endDate
) {}
