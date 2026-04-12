package com.jobportal.applicationservice.service;

import com.jobportal.applicationservice.exception.ApplicationServiceException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Service
public class ApplicationEligibilityServiceImpl implements ApplicationEligibilityService {

    private final RestTemplate restTemplate;
    private final String jobServiceUrl;

    public ApplicationEligibilityServiceImpl(
            RestTemplate restTemplate,
            @Value("${services.job-service.url:http://localhost:8081}") String jobServiceUrl) {
        this.restTemplate = restTemplate;
        this.jobServiceUrl = jobServiceUrl;
    }

    @Override
    public JobEligibility getEligibleJob(Long jobId) {
        try {
            ResponseEntity<JobDetailsResponse> response = restTemplate.getForEntity(
                    jobServiceUrl + "/api/jobs/{id}",
                    JobDetailsResponse.class,
                    jobId
            );
            JobDetailsResponse body = response.getBody();
            if (!response.getStatusCode().is2xxSuccessful() || body == null || body.id() == null) {
                throw new ApplicationServiceException(
                        "APPLICATION_UPSTREAM_FAILED",
                        "Failed to validate job via job-service",
                        502
                );
            }
            if (!"ACTIVE".equalsIgnoreCase(body.status())) {
                throw new ApplicationServiceException(
                        "APPLICATION_JOB_NOT_ELIGIBLE",
                        "Only active jobs can accept applications",
                        400
                );
            }
            return new JobEligibility(body.id(), body.employerId(), body.title());
        } catch (HttpClientErrorException.NotFound ex) {
            throw new ApplicationServiceException(
                    "APPLICATION_JOB_NOT_FOUND",
                    "Job not found",
                    404
            );
        } catch (HttpClientErrorException ex) {
            throw new ApplicationServiceException(
                    "APPLICATION_UPSTREAM_FAILED",
                    "Failed to validate job via job-service",
                    502
            );
        } catch (RestClientException ex) {
            throw new ApplicationServiceException(
                    "APPLICATION_UPSTREAM_FAILED",
                    "Failed to validate job via job-service",
                    502
            );
        }
    }
}
