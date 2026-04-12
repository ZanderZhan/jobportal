package com.jobportal.profileservice.dto;

import java.util.List;

public record ProfileCompletenessResponse(
        int completedFields,
        int totalFields,
        int percentage,
        boolean complete,
        List<String> missingFields
) {}
