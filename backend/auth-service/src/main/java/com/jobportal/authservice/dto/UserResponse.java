package com.jobportal.authservice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.jobportal.authservice.entity.User;
import com.jobportal.authservice.entity.UserType;

import java.util.UUID;

public record UserResponse(
    @JsonProperty("id") UUID id,
    @JsonProperty("email") String email,
    @JsonProperty("name") String name,
    @JsonProperty("role") String role,
    @JsonProperty("userType") String userType
) {
    public static UserResponse fromEntity(User user) {
        return new UserResponse(
            user.getId(),
            user.getEmail(),
            user.getName(),
            user.getRole().name(),
            user.getUserType().name()
        );
    }
}
