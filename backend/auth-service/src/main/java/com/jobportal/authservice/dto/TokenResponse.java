package com.jobportal.authservice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record TokenResponse(
    @JsonProperty("accessToken") String accessToken,
    @JsonProperty("refreshToken") String refreshToken,
    @JsonProperty("tokenType") String tokenType,
    @JsonProperty("expiresIn") long expiresIn,
    UserResponse user
) {
    public TokenResponse(String accessToken, String refreshToken, long expiresIn, UserResponse user) {
        this(accessToken, refreshToken, "Bearer", expiresIn, user);
    }
}
