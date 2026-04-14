package com.jobportal.applicationservice.service;

import com.jobportal.applicationservice.entity.ApplicationStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ApplicationStatusPolicyServiceTest {

    private ApplicationStatusPolicyServiceImpl applicationStatusPolicyService;

    @BeforeEach
    void setUp() {
        applicationStatusPolicyService = new ApplicationStatusPolicyServiceImpl();
    }

    @Test
    void canWithdraw_ShouldAllowSubmittedAndUnderReview() {
        assertTrue(applicationStatusPolicyService.canWithdraw(ApplicationStatus.SUBMITTED));
        assertTrue(applicationStatusPolicyService.canWithdraw(ApplicationStatus.UNDER_REVIEW));
    }

    @Test
    void canWithdraw_ShouldRejectTerminalAndInterviewStates() {
        assertFalse(applicationStatusPolicyService.canWithdraw(ApplicationStatus.INTERVIEW));
        assertFalse(applicationStatusPolicyService.canWithdraw(ApplicationStatus.HIRED));
        assertFalse(applicationStatusPolicyService.canWithdraw(ApplicationStatus.REJECTED));
        assertFalse(applicationStatusPolicyService.canWithdraw(ApplicationStatus.WITHDRAWN));
    }

    @Test
    void canTransition_ShouldAllowExpectedEmployerWorkflow() {
        assertTrue(applicationStatusPolicyService.canTransition(ApplicationStatus.SUBMITTED, ApplicationStatus.UNDER_REVIEW));
        assertTrue(applicationStatusPolicyService.canTransition(ApplicationStatus.SUBMITTED, ApplicationStatus.REJECTED));
        assertTrue(applicationStatusPolicyService.canTransition(ApplicationStatus.UNDER_REVIEW, ApplicationStatus.INTERVIEW));
        assertTrue(applicationStatusPolicyService.canTransition(ApplicationStatus.UNDER_REVIEW, ApplicationStatus.HIRED));
        assertTrue(applicationStatusPolicyService.canTransition(ApplicationStatus.INTERVIEW, ApplicationStatus.HIRED));
    }

    @Test
    void canTransition_ShouldRejectInvalidOrTerminalTransitions() {
        assertFalse(applicationStatusPolicyService.canTransition(ApplicationStatus.SUBMITTED, ApplicationStatus.HIRED));
        assertFalse(applicationStatusPolicyService.canTransition(ApplicationStatus.INTERVIEW, ApplicationStatus.UNDER_REVIEW));
        assertFalse(applicationStatusPolicyService.canTransition(ApplicationStatus.WITHDRAWN, ApplicationStatus.UNDER_REVIEW));
        assertFalse(applicationStatusPolicyService.canTransition(ApplicationStatus.HIRED, ApplicationStatus.REJECTED));
        assertFalse(applicationStatusPolicyService.canTransition(ApplicationStatus.SUBMITTED, ApplicationStatus.SUBMITTED));
    }
}
