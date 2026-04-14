package com.jobportal.notificationservice.dto;

public record ResolvedRecipient(
        String userId,
        String email,
        String name,
        String role
) {
}
