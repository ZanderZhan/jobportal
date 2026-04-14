package com.jobportal.applicationservice.service;

import com.jobportal.applicationservice.exception.ApplicationServiceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ApplicationEligibilityServiceTest {

    @Mock
    private RestTemplate restTemplate;

    private ApplicationEligibilityServiceImpl applicationEligibilityService;

    @BeforeEach
    void setUp() {
        applicationEligibilityService = new ApplicationEligibilityServiceImpl(
                restTemplate,
                "http://job-service:8081",
                2,
                0,
                2,
                1
        );
    }

    @Test
    void getEligibleJob_WhenJobIsActive_ShouldReturnEligibilitySnapshot() {
        when(restTemplate.getForEntity(
                "http://job-service:8081/api/jobs/{id}",
                JobDetailsResponse.class,
                12L
        )).thenReturn(ResponseEntity.ok(new JobDetailsResponse(12L, "employer-9", "Platform Engineer", "ACTIVE")));

        JobEligibility eligibility = applicationEligibilityService.getEligibleJob(12L);

        assertEquals(12L, eligibility.jobId());
        assertEquals("employer-9", eligibility.employerId());
        assertEquals("Platform Engineer", eligibility.title());
    }

    @Test
    void getEligibleJob_WhenJobIsNotActive_ShouldThrowBadRequest() {
        when(restTemplate.getForEntity(
                "http://job-service:8081/api/jobs/{id}",
                JobDetailsResponse.class,
                12L
        )).thenReturn(ResponseEntity.ok(new JobDetailsResponse(12L, "employer-9", "Platform Engineer", "CLOSED")));

        ApplicationServiceException ex = assertThrows(
                ApplicationServiceException.class,
                () -> applicationEligibilityService.getEligibleJob(12L)
        );

        assertEquals("APPLICATION_JOB_NOT_ELIGIBLE", ex.getErrorCode());
        assertEquals(400, ex.getHttpStatus());
    }

    @Test
    void getEligibleJob_WhenJobDoesNotExist_ShouldThrowNotFound() {
        when(restTemplate.getForEntity(
                "http://job-service:8081/api/jobs/{id}",
                JobDetailsResponse.class,
                12L
        )).thenThrow(HttpClientErrorException.create(
                HttpStatus.NOT_FOUND,
                "Not Found",
                HttpHeaders.EMPTY,
                new byte[0],
                StandardCharsets.UTF_8
        ));

        ApplicationServiceException ex = assertThrows(
                ApplicationServiceException.class,
                () -> applicationEligibilityService.getEligibleJob(12L)
        );

        assertEquals("APPLICATION_JOB_NOT_FOUND", ex.getErrorCode());
        assertEquals(404, ex.getHttpStatus());
    }

    @Test
    void getEmployerOwnedJob_WhenEmployerOwnsJob_ShouldReturnJobDetails() {
        when(restTemplate.getForEntity(
                "http://job-service:8081/api/jobs/{id}",
                JobDetailsResponse.class,
                15L
        )).thenReturn(ResponseEntity.ok(new JobDetailsResponse(15L, "employer-7", "Security Analyst", "ACTIVE")));

        JobDetailsResponse response = applicationEligibilityService.getEmployerOwnedJob(15L, "employer-7");

        assertEquals(15L, response.id());
        assertEquals("employer-7", response.employerId());
    }

    @Test
    void getEmployerOwnedJob_WhenEmployerDoesNotOwnJob_ShouldThrowForbidden() {
        when(restTemplate.getForEntity(
                "http://job-service:8081/api/jobs/{id}",
                JobDetailsResponse.class,
                15L
        )).thenReturn(ResponseEntity.ok(new JobDetailsResponse(15L, "employer-7", "Security Analyst", "ACTIVE")));

        ApplicationServiceException ex = assertThrows(
                ApplicationServiceException.class,
                () -> applicationEligibilityService.getEmployerOwnedJob(15L, "employer-8")
        );

        assertEquals("APPLICATION_FORBIDDEN", ex.getErrorCode());
        assertEquals(403, ex.getHttpStatus());
    }

    @Test
    void getEligibleJob_WhenFirstAttemptFails_ShouldRetryAndReturnJob() {
        when(restTemplate.getForEntity(
                "http://job-service:8081/api/jobs/{id}",
                JobDetailsResponse.class,
                22L
        ))
                .thenThrow(new org.springframework.web.client.ResourceAccessException("timeout"))
                .thenReturn(ResponseEntity.ok(new JobDetailsResponse(22L, "employer-4", "Data Engineer", "ACTIVE")));

        JobEligibility eligibility = applicationEligibilityService.getEligibleJob(22L);

        assertEquals(22L, eligibility.jobId());
        assertEquals("employer-4", eligibility.employerId());
        verify(restTemplate, times(2)).getForEntity(
                "http://job-service:8081/api/jobs/{id}",
                JobDetailsResponse.class,
                22L
        );
    }

    @Test
    void getEligibleJob_WhenCircuitIsOpen_ShouldReturnServiceUnavailable() {
        when(restTemplate.getForEntity(
                "http://job-service:8081/api/jobs/{id}",
                JobDetailsResponse.class,
                27L
        )).thenThrow(new org.springframework.web.client.ResourceAccessException("timeout"));

        ApplicationServiceException firstFailure = assertThrows(
                ApplicationServiceException.class,
                () -> applicationEligibilityService.getEligibleJob(27L)
        );
        assertEquals("APPLICATION_UPSTREAM_FAILED", firstFailure.getErrorCode());

        ApplicationServiceException circuitOpen = assertThrows(
                ApplicationServiceException.class,
                () -> applicationEligibilityService.getEligibleJob(27L)
        );

        assertEquals("APPLICATION_JOB_SERVICE_UNAVAILABLE", circuitOpen.getErrorCode());
        assertEquals(503, circuitOpen.getHttpStatus());
    }
}
