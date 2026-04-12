package com.jobportal.applicationservice.service;

import com.jobportal.applicationservice.exception.ApplicationServiceException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class ApplicationEligibilityServiceImpl implements ApplicationEligibilityService {

    private final RestTemplate restTemplate;
    private final String jobServiceUrl;
    private final int retryMaxAttempts;
    private final long retryBackoffMs;
    private final int circuitFailureThreshold;
    private final long circuitOpenSeconds;
    private final AtomicInteger consecutiveUpstreamFailures = new AtomicInteger();
    private volatile Instant circuitOpenedAt;

    public ApplicationEligibilityServiceImpl(
            RestTemplate restTemplate,
            @Value("${services.job-service.url:http://localhost:8081}") String jobServiceUrl,
            @Value("${services.job-service.retry.max-attempts:2}") int retryMaxAttempts,
            @Value("${services.job-service.retry.backoff-ms:100}") long retryBackoffMs,
            @Value("${services.job-service.circuit-breaker.failure-threshold:3}") int circuitFailureThreshold,
            @Value("${services.job-service.circuit-breaker.open-seconds:30}") long circuitOpenSeconds) {
        this.restTemplate = restTemplate;
        this.jobServiceUrl = jobServiceUrl;
        this.retryMaxAttempts = retryMaxAttempts;
        this.retryBackoffMs = retryBackoffMs;
        this.circuitFailureThreshold = circuitFailureThreshold;
        this.circuitOpenSeconds = circuitOpenSeconds;
    }

    @Override
    public JobEligibility getEligibleJob(Long jobId) {
        try {
            JobDetailsResponse body = getJob(jobId);
            if (!"ACTIVE".equalsIgnoreCase(body.status())) {
                throw new ApplicationServiceException(
                        "APPLICATION_JOB_NOT_ELIGIBLE",
                        "Only active jobs can accept applications",
                        400
                );
            }
            return new JobEligibility(body.id(), body.employerId(), body.title());
        } catch (ApplicationServiceException ex) {
            throw ex;
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

    @Override
    public JobDetailsResponse getEmployerOwnedJob(Long jobId, String employerId) {
        try {
            JobDetailsResponse body = getJob(jobId);
            if (body.employerId() == null || !body.employerId().equals(employerId)) {
                throw new ApplicationServiceException(
                        "APPLICATION_FORBIDDEN",
                        "You do not have access to applications for this job",
                        403
                );
            }
            return body;
        } catch (ApplicationServiceException ex) {
            throw ex;
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

    private JobDetailsResponse getJob(Long jobId) {
        if (isCircuitOpen()) {
            throw new ApplicationServiceException(
                    "APPLICATION_JOB_SERVICE_UNAVAILABLE",
                    "Job validation is temporarily unavailable",
                    503
            );
        }

        RestClientException lastException = null;
        for (int attempt = 1; attempt <= retryMaxAttempts; attempt++) {
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
                resetCircuit();
                return body;
            } catch (HttpClientErrorException ex) {
                resetCircuit();
                throw ex;
            } catch (RestClientException ex) {
                lastException = ex;
                recordFailure();
                if (attempt >= retryMaxAttempts) {
                    break;
                }
                sleepBeforeRetry();
            }
        }

        throw new ApplicationServiceException(
                "APPLICATION_UPSTREAM_FAILED",
                "Failed to validate job via job-service",
                502
        );
    }

    private boolean isCircuitOpen() {
        Instant openedAt = circuitOpenedAt;
        if (openedAt == null) {
            return false;
        }
        if (openedAt.plusSeconds(circuitOpenSeconds).isAfter(Instant.now())) {
            return true;
        }
        circuitOpenedAt = null;
        consecutiveUpstreamFailures.set(0);
        return false;
    }

    private void resetCircuit() {
        circuitOpenedAt = null;
        consecutiveUpstreamFailures.set(0);
    }

    private void recordFailure() {
        if (consecutiveUpstreamFailures.incrementAndGet() >= circuitFailureThreshold) {
            circuitOpenedAt = Instant.now();
        }
    }

    private void sleepBeforeRetry() {
        try {
            Thread.sleep(retryBackoffMs);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }
}
