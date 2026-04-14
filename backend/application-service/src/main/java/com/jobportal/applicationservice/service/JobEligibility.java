package com.jobportal.applicationservice.service;

public record JobEligibility(
        Long jobId,
        String employerId,
        String title
) {}
