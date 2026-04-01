package com.jobportal.authservice.dto;

import jakarta.validation.constraints.NotBlank;

public record OAuthCallbackRequest(
    @NotBlank(message = "Code is required")
    String code,

    @NotBlank(message = "State is required")
    String state,

    String redirectUri
) {}
