package com.jobportal.profileservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record PortfolioLinkRequest(
        @NotBlank(message = "Portfolio link label is required")
        @Size(max = 100, message = "Portfolio link label must be less than 100 characters")
        String label,

        @NotBlank(message = "Portfolio link URL is required")
        @Pattern(
                regexp = "^https?://\\S+$",
                message = "Portfolio link URL must start with http:// or https://"
        )
        @Size(max = 500, message = "Portfolio link URL must be less than 500 characters")
        String url
) {}
