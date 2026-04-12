package com.jobportal.notificationservice.service;

import com.jobportal.notificationservice.contract.ApplicationStatusChangedEvent;
import com.jobportal.notificationservice.contract.ApplicationSubmittedEvent;
import com.jobportal.notificationservice.contract.EmployerVerifiedEvent;
import com.jobportal.notificationservice.contract.JobPostedEvent;
import com.jobportal.notificationservice.dto.EventNotificationRequest;
import com.jobportal.notificationservice.entity.NotificationEventType;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

import java.util.Map;

import static com.jobportal.notificationservice.config.NotificationTopologyProperties.APPLICATION_STATUS_CHANGED_QUEUE;
import static com.jobportal.notificationservice.config.NotificationTopologyProperties.APPLICATION_SUBMITTED_QUEUE;
import static com.jobportal.notificationservice.config.NotificationTopologyProperties.EMPLOYER_VERIFIED_QUEUE;
import static com.jobportal.notificationservice.config.NotificationTopologyProperties.JOB_POSTED_QUEUE;

@Service
public class DomainEventConsumerService {

    private final NotificationWorkflowService notificationWorkflowService;

    public DomainEventConsumerService(NotificationWorkflowService notificationWorkflowService) {
        this.notificationWorkflowService = notificationWorkflowService;
    }

    @RabbitListener(queues = APPLICATION_SUBMITTED_QUEUE)
    public void onApplicationSubmitted(ApplicationSubmittedEvent event) {
        // This path creates a confirmation-style notification for the applicant.
        notificationWorkflowService.handleEvent(new EventNotificationRequest(
                "application-submitted-" + event.applicationId(),
                NotificationEventType.APPLICATION_SUBMITTED,
                event.studentId(),
                null,
                event.timestamp(),
                Map.of(
                        "applicationId", String.valueOf(event.applicationId()),
                        "jobId", String.valueOf(event.jobId())
                )
        ));
    }

    @RabbitListener(queues = APPLICATION_STATUS_CHANGED_QUEUE)
    public void onApplicationStatusChanged(ApplicationStatusChangedEvent event) {
        // TODO: recipientUserId should be the student's userId, not the applicationId.
        //       Enrich ApplicationStatusChangedEvent with a studentId field once the
        //       application-service exposes that data so the correct user is notified.
        notificationWorkflowService.handleEvent(new EventNotificationRequest(
                "application-status-changed-" + event.applicationId() + "-" + event.newStatus(),
                NotificationEventType.APPLICATION_STATUS_CHANGED,
                event.applicationId(),
                null,
                event.timestamp(),
                Map.of(
                        "applicationId", String.valueOf(event.applicationId()),
                        "oldStatus", event.oldStatus(),
                        "newStatus", event.newStatus()
                )
        ));
    }

    @RabbitListener(queues = JOB_POSTED_QUEUE)
    public void onJobPosted(JobPostedEvent event) {
        notificationWorkflowService.handleEvent(new EventNotificationRequest(
                "job-posted-" + event.jobId(),
                NotificationEventType.JOB_POSTED,
                event.employerId(),
                null,
                event.timestamp(),
                Map.of(
                        "jobId", String.valueOf(event.jobId()),
                        "title", event.title()
                )
        ));
    }

    @RabbitListener(queues = EMPLOYER_VERIFIED_QUEUE)
    public void onEmployerVerified(EmployerVerifiedEvent event) {
        notificationWorkflowService.handleEvent(new EventNotificationRequest(
                "employer-verified-" + event.employerId() + "-" + event.verificationStatus(),
                NotificationEventType.EMPLOYER_VERIFIED,
                event.employerId(),
                null,
                event.timestamp(),
                Map.of("verificationStatus", event.verificationStatus())
        ));
    }
}
