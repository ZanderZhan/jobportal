package com.jobportal.applicationservice.service;

import com.jobportal.applicationservice.dto.ApplicationCreateRequest;
import com.jobportal.applicationservice.dto.ApplicationResponse;
import com.jobportal.applicationservice.dto.ApplicationStatusUpdateRequest;
import com.jobportal.applicationservice.config.CorrelationIdMdcFilter;
import com.jobportal.applicationservice.event.ApplicationStatusUpdatedEvent;
import com.jobportal.applicationservice.event.ApplicationSubmittedEvent;
import com.jobportal.applicationservice.event.ApplicationWithdrawnEvent;
import com.jobportal.applicationservice.entity.Application;
import com.jobportal.applicationservice.entity.ApplicationStatus;
import com.jobportal.applicationservice.exception.ApplicationServiceException;
import com.jobportal.applicationservice.repository.ApplicationRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Instant;
import java.util.List;

@Service
@Transactional
public class ApplicationServiceImpl implements ApplicationService {

    private static final Logger log = LoggerFactory.getLogger(ApplicationServiceImpl.class);

    private static final String STUDENT_ROLE = "STUDENT";
    private static final String JOB_SEEKER_ROLE = "JOB_SEEKER";
    private static final String EMPLOYER_ROLE = "EMPLOYER";
    private static final String HIRING_ROLE = "HIRING";

    private final ApplicationRepository applicationRepository;
    private final ApplicationEligibilityService applicationEligibilityService;
    private final ApplicationStatusPolicyService applicationStatusPolicyService;
    private final ApplicationEventPublisher applicationEventPublisher;

    public ApplicationServiceImpl(
            ApplicationRepository applicationRepository,
            ApplicationEligibilityService applicationEligibilityService,
            ApplicationStatusPolicyService applicationStatusPolicyService,
            ApplicationEventPublisher applicationEventPublisher) {
        this.applicationRepository = applicationRepository;
        this.applicationEligibilityService = applicationEligibilityService;
        this.applicationStatusPolicyService = applicationStatusPolicyService;
        this.applicationEventPublisher = applicationEventPublisher;
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
            log.info(
                    "Application submitted applicationId={} studentId={} jobId={} correlationId={}",
                    savedApplication.getId(),
                    savedApplication.getStudentId(),
                    savedApplication.getJobId(),
                    correlationId()
            );
            publishAfterCommit(() -> applicationEventPublisher.publishSubmitted(
                    new ApplicationSubmittedEvent(
                            savedApplication.getId(),
                            savedApplication.getStudentId(),
                            savedApplication.getJobId(),
                            Instant.now()
                    )
            ));
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
        Application savedApplication = applicationRepository.saveAndFlush(application);
        log.info(
                "Application withdrawn applicationId={} studentId={} jobId={} correlationId={}",
                savedApplication.getId(),
                savedApplication.getStudentId(),
                savedApplication.getJobId(),
                correlationId()
        );
        publishAfterCommit(() -> applicationEventPublisher.publishWithdrawn(
                new ApplicationWithdrawnEvent(
                        savedApplication.getId(),
                        savedApplication.getStudentId(),
                        savedApplication.getJobId(),
                        Instant.now()
                )
        ));
        return ApplicationResponse.fromEntity(savedApplication);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ApplicationResponse> getEmployerApplicationsForJob(Long jobId, String employerId, String userRole) {
        String normalizedEmployerId = requireEmployerContext(employerId, userRole);
        applicationEligibilityService.getEmployerOwnedJob(jobId, normalizedEmployerId);
        return applicationRepository.findAllByJobIdOrderBySubmittedAtDesc(jobId)
                .stream()
                .map(ApplicationResponse::fromEntity)
                .toList();
    }

    @Override
    public ApplicationResponse updateApplicationStatus(
            Long applicationId,
            String employerId,
            String userRole,
            ApplicationStatusUpdateRequest request) {
        String normalizedEmployerId = requireEmployerContext(employerId, userRole);
        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ApplicationServiceException(
                        "APPLICATION_NOT_FOUND",
                        "Application not found",
                        404
                ));

        applicationEligibilityService.getEmployerOwnedJob(application.getJobId(), normalizedEmployerId);

        if (!applicationStatusPolicyService.canTransition(application.getStatus(), request.status())) {
            throw new ApplicationServiceException(
                    "APPLICATION_INVALID_STATUS_TRANSITION",
                    "This status transition is not allowed",
                    409
            );
        }

        ApplicationStatus previousStatus = application.getStatus();
        application.updateStatus(
                request.status(),
                normalizedEmployerId,
                buildEmployerReason(previousStatus, request.status(), request.reason())
        );
        Application savedApplication = applicationRepository.saveAndFlush(application);
        log.info(
                "Application status updated applicationId={} studentId={} employerId={} jobId={} oldStatus={} newStatus={} correlationId={}",
                savedApplication.getId(),
                savedApplication.getStudentId(),
                normalizedEmployerId,
                savedApplication.getJobId(),
                previousStatus,
                savedApplication.getStatus(),
                correlationId()
        );
        publishAfterCommit(() -> applicationEventPublisher.publishStatusUpdated(
                new ApplicationStatusUpdatedEvent(
                        savedApplication.getId(),
                        savedApplication.getStudentId(),
                        normalizedEmployerId,
                        savedApplication.getJobId(),
                        previousStatus.name(),
                        savedApplication.getStatus().name(),
                        Instant.now()
                )
        ));
        return ApplicationResponse.fromEntity(savedApplication);
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

    private String requireEmployerContext(String employerId, String userRole) {
        String normalizedEmployerId = normalize(employerId);
        String normalizedRole = normalize(userRole);

        if (normalizedEmployerId == null || normalizedRole == null) {
            throw new ApplicationServiceException(
                    "APPLICATION_UNAUTHORIZED",
                    "Authenticated user context is required",
                    401
            );
        }
        if (!EMPLOYER_ROLE.equalsIgnoreCase(normalizedRole) && !HIRING_ROLE.equalsIgnoreCase(normalizedRole)) {
            throw new ApplicationServiceException(
                    "APPLICATION_FORBIDDEN",
                    "Only employers can access this operation",
                    403
            );
        }

        return normalizedEmployerId;
    }

    private String buildEmployerReason(ApplicationStatus currentStatus, ApplicationStatus nextStatus, String reason) {
        String normalizedReason = normalize(reason);
        if (normalizedReason != null) {
            return normalizedReason;
        }
        return "Status updated from " + currentStatus + " to " + nextStatus;
    }

    private void publishAfterCommit(Runnable action) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    runEventPublication(action);
                }
            });
            return;
        }

        runEventPublication(action);
    }

    private void runEventPublication(Runnable action) {
        try {
            action.run();
        } catch (RuntimeException ex) {
            log.warn(
                    "Application event publication failed after persistence correlationId={}",
                    correlationId(),
                    ex
            );
        }
    }

    private String correlationId() {
        return MDC.get(CorrelationIdMdcFilter.MDC_KEY);
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }
}
