package com.jobportal.profileservice.dto;

import com.jobportal.profileservice.entity.ProfileVisibility;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

public record StudentProfileUpdateRequest(
        @Size(max = 255, message = "Headline must be less than 255 characters")
        String headline,

        String bio,

        @Size(max = 255, message = "Location must be less than 255 characters")
        String location,

        @Size(max = 64, message = "Phone must be less than 64 characters")
        @Pattern(
                regexp = "^[0-9+()\\-\\s]{0,64}$",
                message = "Phone contains invalid characters"
        )
        String phone,

        ProfileVisibility visibility,

        @Size(max = 64, message = "Job search status must be less than 64 characters")
        String jobSearchStatus,

        @Valid
        List<@Size(max = 100, message = "Skill name must be less than 100 characters") String> skills,

        @Valid
        List<EducationEntryRequest> education,

        @Valid
        List<ExperienceEntryRequest> experience,

        @Valid
        List<PortfolioLinkRequest> portfolioLinks
) {}
