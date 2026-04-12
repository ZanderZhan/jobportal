package com.jobportal.applicationservice.service;

public interface ApplicationEligibilityService {

    JobEligibility getEligibleJob(Long jobId);

    JobDetailsResponse getEmployerOwnedJob(Long jobId, String employerId);
}
