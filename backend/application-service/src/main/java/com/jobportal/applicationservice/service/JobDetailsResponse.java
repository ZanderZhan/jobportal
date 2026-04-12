package com.jobportal.applicationservice.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record JobDetailsResponse(
        Long id,
        String employerId,
        String title,
        String status
) {}
