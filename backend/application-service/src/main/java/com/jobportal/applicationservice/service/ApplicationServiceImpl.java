package com.jobportal.applicationservice.service;

import com.jobportal.applicationservice.dto.ApplicationCreateRequest;
import com.jobportal.applicationservice.dto.ApplicationResponse;
import com.jobportal.applicationservice.entity.Application;
import com.jobportal.applicationservice.exception.ApplicationServiceException;
import com.jobportal.applicationservice.repository.ApplicationRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class ApplicationServiceImpl implements ApplicationService {

    private static final String STUDENT_ROLE = "STUDENT";
    private static final String JOB_SEEKER_ROLE = "JOB_SEEKER";

    private final ApplicationRepository applicationRepository;
    private final ApplicationEligibilityService applicationEligibilityService;
    private final ApplicationStatusPolicyService applicationStatusPolicyService;

    public ApplicationServiceImpl(
            ApplicationRepository applicationRepository,
            ApplicationEligibilityService applicationEligibilityService,
            ApplicationStatusPolicyService applicationStatusPolicyService) {
        this.applicationRepository = applicationRepository;
        this.applicationEligibilityService = applicationEligibilityService;
        this.applicationStatusPolicyService = applicationStatusPolicyService;
    }

    @Override
    public ApplicationResponse submitApplication(String studentId, String userRole, ApplicationCreateRequest request) {
        String normalizedStudentId = requireStudentContext(studentId, userRole);
        if (applicationRepository.existsByStudentIdAndJobId(normalizedStudentId, request.jobId())) {
            throw new ApplicationServiceException(
                    "APPLICATION_DUPLICATE",
                    "You have already applied to this job",
                    409
            );
        }

        JobEligibility eligibleJob = applicationEligibilityService.getEligibleJob(request.jobId());
        Application application = Application.createSubmitted(
                normalizedStudentId,
                request.jobId(),
                eligibleJob.employerId(),
                eligibleJob.title(),
                request.resumeReference().trim(),
                normalizedStudentId
        );

        try {
            Application savedApplication = applicationRepository.save(application);
            return ApplicationResponse.fromEntity(savedApplication);
        } catch (DataIntegrityViolationException ex) {
            if (!applicationRepository.existsByStudentIdAndJobId(normalizedStudentId, request.jobId())) {
                throw new ApplicationServiceException(
                        "APPLICATION_PERSISTENCE_FAILED",
                        "Failed to save application",
                        500
                );
            }
            throw new ApplicationServiceException(
                "APPLICATION_DUPLICATE",
                "You have already applied to this job",
                    409
            );
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<ApplicationResponse> getStudentApplications(String studentId, String userRole) {
        String normalizedStudentId = requireStudentContext(studentId, userRole);
        return applicationRepository.findAllByStudentIdOrderBySubmittedAtDesc(normalizedStudentId)
                .stream()
                .map(ApplicationResponse::fromEntity)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ApplicationResponse getStudentApplicationById(Long applicationId, String studentId, String userRole) {
        String normalizedStudentId = requireStudentContext(studentId, userRole);
        Application application = findStudentApplication(applicationId, normalizedStudentId);
        return ApplicationResponse.fromEntity(application);
    }

    @Override
    public ApplicationResponse withdrawApplication(Long applicationId, String studentId, String userRole) {
        String normalizedStudentId = requireStudentContext(studentId, userRole);
        Application application = findStudentApplication(applicationId, normalizedStudentId);

        if (!applicationStatusPolicyService.canWithdraw(application.getStatus())) {
            throw new ApplicationServiceException(
                    "APPLICATION_WITHDRAWAL_NOT_ALLOWED",
                    "This application can no longer be withdrawn",
                    409
            );
        }

        application.withdraw(normalizedStudentId, "Application withdrawn by student");
        return ApplicationResponse.fromEntity(application);
    }

    private Application findStudentApplication(Long applicationId, String studentId) {
        return applicationRepository.findByIdAndStudentId(applicationId, studentId)
                .orElseThrow(() -> new ApplicationServiceException(
                        "APPLICATION_NOT_FOUND",
                        "Application not found",
                        404
                ));
    }

    private String requireStudentContext(String studentId, String userRole) {
        String normalizedStudentId = normalize(studentId);
        String normalizedRole = normalize(userRole);

        if (normalizedStudentId == null || normalizedRole == null) {
            throw new ApplicationServiceException(
                    "APPLICATION_UNAUTHORIZED",
                    "Authenticated user context is required",
                    401
            );
        }
        if (!STUDENT_ROLE.equalsIgnoreCase(normalizedRole) && !JOB_SEEKER_ROLE.equalsIgnoreCase(normalizedRole)) {
            throw new ApplicationServiceException(
                    "APPLICATION_FORBIDDEN",
                    "Only students can access this operation",
                    403
            );
        }

        return normalizedStudentId;
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }
}
