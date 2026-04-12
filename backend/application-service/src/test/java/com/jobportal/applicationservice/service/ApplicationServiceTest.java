package com.jobportal.applicationservice.service;

import com.jobportal.applicationservice.dto.ApplicationCreateRequest;
import com.jobportal.applicationservice.dto.ApplicationResponse;
import com.jobportal.applicationservice.entity.Application;
import com.jobportal.applicationservice.entity.ApplicationStatus;
import com.jobportal.applicationservice.exception.ApplicationServiceException;
import com.jobportal.applicationservice.repository.ApplicationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ApplicationServiceTest {

    @Mock
    private ApplicationRepository applicationRepository;

    @Mock
    private ApplicationEligibilityService applicationEligibilityService;

    @Mock
    private ApplicationStatusPolicyService applicationStatusPolicyService;

    private ApplicationServiceImpl applicationService;

    @BeforeEach
    void setUp() {
        applicationService = new ApplicationServiceImpl(
                applicationRepository,
                applicationEligibilityService,
                applicationStatusPolicyService
        );
    }

    @Test
    void submitApplication_ShouldPersistSubmittedApplication() {
        ApplicationCreateRequest request = new ApplicationCreateRequest(5L, "resume://student-1.pdf");
        when(applicationRepository.existsByStudentIdAndJobId("student-1", 5L)).thenReturn(false);
        when(applicationEligibilityService.getEligibleJob(5L))
                .thenReturn(new JobEligibility(5L, "employer-2", "Backend Engineer"));
        when(applicationRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        ApplicationResponse response = applicationService.submitApplication("student-1", "JOB_SEEKER", request);

        assertEquals("student-1", response.studentId());
        assertEquals(5L, response.jobId());
        assertEquals("employer-2", response.employerId());
        assertEquals("Backend Engineer", response.jobTitle());
        assertEquals("resume://student-1.pdf", response.resumeReference());
        assertEquals(1, response.timeline().size());
        assertEquals("Application submitted", response.timeline().get(0).reason());
        assertNotNull(response.submittedAt());
        verify(applicationRepository).save(any());
    }

    @Test
    void submitApplication_WhenRoleIsNotStudent_ShouldThrowForbidden() {
        ApplicationCreateRequest request = new ApplicationCreateRequest(5L, "resume://student-1.pdf");

        ApplicationServiceException ex = assertThrows(
                ApplicationServiceException.class,
                () -> applicationService.submitApplication("student-1", "HIRING", request)
        );

        assertEquals("APPLICATION_FORBIDDEN", ex.getErrorCode());
        assertEquals(403, ex.getHttpStatus());
    }

    @Test
    void submitApplication_WhenDuplicateExists_ShouldThrowConflict() {
        ApplicationCreateRequest request = new ApplicationCreateRequest(5L, "resume://student-1.pdf");
        when(applicationRepository.existsByStudentIdAndJobId("student-1", 5L)).thenReturn(true);

        ApplicationServiceException ex = assertThrows(
                ApplicationServiceException.class,
                () -> applicationService.submitApplication("student-1", "JOB_SEEKER", request)
        );

        assertEquals("APPLICATION_DUPLICATE", ex.getErrorCode());
        assertEquals(409, ex.getHttpStatus());
    }

    @Test
    void submitApplication_WhenPersistenceFailsForNonDuplicate_ShouldThrowServerError() {
        ApplicationCreateRequest request = new ApplicationCreateRequest(5L, "resume://student-1.pdf");
        when(applicationRepository.existsByStudentIdAndJobId("student-1", 5L))
                .thenReturn(false, false);
        when(applicationEligibilityService.getEligibleJob(5L))
                .thenReturn(new JobEligibility(5L, null, "Backend Engineer"));
        when(applicationRepository.save(any()))
                .thenThrow(new DataIntegrityViolationException("not-null violation"));

        ApplicationServiceException ex = assertThrows(
                ApplicationServiceException.class,
                () -> applicationService.submitApplication("student-1", "JOB_SEEKER", request)
        );

        assertEquals("APPLICATION_PERSISTENCE_FAILED", ex.getErrorCode());
        assertEquals(500, ex.getHttpStatus());
    }

    @Test
    void getStudentApplications_ShouldReturnMappedApplications() {
        Application first = Application.createSubmitted(
                "student-1",
                8L,
                "employer-1",
                "QA Engineer",
                "resume://student-1.pdf",
                "student-1"
        );
        Application second = Application.createSubmitted(
                "student-1",
                6L,
                "employer-2",
                "Frontend Engineer",
                "resume://student-1.pdf",
                "student-1"
        );
        when(applicationRepository.findAllByStudentIdOrderBySubmittedAtDesc("student-1"))
                .thenReturn(List.of(first, second));

        List<ApplicationResponse> response = applicationService.getStudentApplications("student-1", "JOB_SEEKER");

        assertEquals(2, response.size());
        assertEquals(8L, response.get(0).jobId());
        assertEquals("QA Engineer", response.get(0).jobTitle());
    }

    @Test
    void getStudentApplicationById_WhenMissing_ShouldThrowNotFound() {
        when(applicationRepository.findByIdAndStudentId(99L, "student-1"))
                .thenReturn(java.util.Optional.empty());

        ApplicationServiceException ex = assertThrows(
                ApplicationServiceException.class,
                () -> applicationService.getStudentApplicationById(99L, "student-1", "JOB_SEEKER")
        );

        assertEquals("APPLICATION_NOT_FOUND", ex.getErrorCode());
        assertEquals(404, ex.getHttpStatus());
    }

    @Test
    void withdrawApplication_WhenAllowed_ShouldUpdateStatusAndTimeline() {
        Application application = Application.createSubmitted(
                "student-1",
                5L,
                "employer-2",
                "Backend Engineer",
                "resume://student-1.pdf",
                "student-1"
        );
        when(applicationRepository.findByIdAndStudentId(15L, "student-1"))
                .thenReturn(java.util.Optional.of(application));
        when(applicationStatusPolicyService.canWithdraw(ApplicationStatus.SUBMITTED))
                .thenReturn(true);

        ApplicationResponse response = applicationService.withdrawApplication(15L, "student-1", "JOB_SEEKER");

        assertEquals(ApplicationStatus.WITHDRAWN, response.status());
        assertEquals(2, response.timeline().size());
        assertEquals("Application withdrawn by student", response.timeline().get(1).reason());
        verify(applicationRepository, never()).save(any());
    }

    @Test
    void withdrawApplication_WhenNotAllowed_ShouldThrowConflict() {
        Application application = Application.createSubmitted(
                "student-1",
                5L,
                "employer-2",
                "Backend Engineer",
                "resume://student-1.pdf",
                "student-1"
        );
        when(applicationRepository.findByIdAndStudentId(15L, "student-1"))
                .thenReturn(java.util.Optional.of(application));
        when(applicationStatusPolicyService.canWithdraw(ApplicationStatus.SUBMITTED))
                .thenReturn(false);

        ApplicationServiceException ex = assertThrows(
                ApplicationServiceException.class,
                () -> applicationService.withdrawApplication(15L, "student-1", "JOB_SEEKER")
        );

        assertEquals("APPLICATION_WITHDRAWAL_NOT_ALLOWED", ex.getErrorCode());
        assertEquals(409, ex.getHttpStatus());
    }
}
