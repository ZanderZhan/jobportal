package com.jobportal.authservice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.jobportal.authservice.entity.User;

import java.util.UUID;

public record UserResponse(
    @JsonProperty("id") UUID id,
    @JsonProperty("email") String email,
    @JsonProperty("name") String name,
    @JsonProperty("role") String role
) {
    public static UserResponse fromEntity(User user) {
        return new UserResponse(
            user.getId(),
            user.getEmail(),
            user.getName(),
            user.getRole().name()
        );
    }
}
