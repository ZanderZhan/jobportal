package com.jobportal.notificationservice.service;

import com.jobportal.notificationservice.config.NotificationRetryProperties;
import com.jobportal.notificationservice.contract.ApplicationStatusChangedEvent;
import com.jobportal.notificationservice.contract.ApplicationSubmittedEvent;
import com.jobportal.notificationservice.contract.ApplicationWithdrawnEvent;
import com.jobportal.notificationservice.contract.EmployerVerifiedEvent;
import com.jobportal.notificationservice.contract.JobPostedEvent;
import com.jobportal.notificationservice.dto.EventNotificationRequest;
import com.jobportal.notificationservice.entity.NotificationEventType;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Service;

import java.util.Map;

import static com.jobportal.notificationservice.config.NotificationTopologyProperties.APPLICATION_STATUS_CHANGED_QUEUE;
import static com.jobportal.notificationservice.config.NotificationTopologyProperties.APPLICATION_STATUS_CHANGED_RETRY_ROUTING_KEY;
import static com.jobportal.notificationservice.config.NotificationTopologyProperties.APPLICATION_SUBMITTED_QUEUE;
import static com.jobportal.notificationservice.config.NotificationTopologyProperties.APPLICATION_SUBMITTED_RETRY_ROUTING_KEY;
import static com.jobportal.notificationservice.config.NotificationTopologyProperties.APPLICATION_WITHDRAWN_QUEUE;
import static com.jobportal.notificationservice.config.NotificationTopologyProperties.APPLICATION_WITHDRAWN_RETRY_ROUTING_KEY;
import static com.jobportal.notificationservice.config.NotificationTopologyProperties.EMPLOYER_VERIFIED_QUEUE;
import static com.jobportal.notificationservice.config.NotificationTopologyProperties.EMPLOYER_VERIFIED_RETRY_ROUTING_KEY;
import static com.jobportal.notificationservice.config.NotificationTopologyProperties.JOB_POSTED_QUEUE;
import static com.jobportal.notificationservice.config.NotificationTopologyProperties.JOB_POSTED_RETRY_ROUTING_KEY;

@Service
public class DomainEventConsumerService {

    private final NotificationWorkflowService notificationWorkflowService;
    private final NotificationRetryPublisher notificationRetryPublisher;
    private final NotificationRetryProperties retryProperties;

    public DomainEventConsumerService(
            NotificationWorkflowService notificationWorkflowService,
            NotificationRetryPublisher notificationRetryPublisher,
            NotificationRetryProperties retryProperties
    ) {
        this.notificationWorkflowService = notificationWorkflowService;
        this.notificationRetryPublisher = notificationRetryPublisher;
        this.retryProperties = retryProperties;
    }

    @RabbitListener(queues = APPLICATION_SUBMITTED_QUEUE)
    public void onApplicationSubmitted(
            ApplicationSubmittedEvent event,
            @Header(name = NotificationRetryPublisher.RETRY_COUNT_HEADER, required = false) Integer retryCount
    ) {
        NotificationProcessingResult result = notificationWorkflowService.handleEventForDelivery(new EventNotificationRequest(
                "application-submitted-" + event.applicationId(),
                NotificationEventType.APPLICATION_SUBMITTED,
                event.studentId(),
                null,
                null,
                event.timestamp(),
                Map.of(
                        "applicationId", String.valueOf(event.applicationId()),
                        "jobId", String.valueOf(event.jobId()),
                        "recipientLabel", "student"
                )
        ));

        handleRecipientWarmupRetry(
                result,
                event,
                retryCount,
                APPLICATION_SUBMITTED_RETRY_ROUTING_KEY
        );
    }

    @RabbitListener(queues = APPLICATION_STATUS_CHANGED_QUEUE)
    public void onApplicationStatusChanged(
            ApplicationStatusChangedEvent event,
            @Header(name = NotificationRetryPublisher.RETRY_COUNT_HEADER, required = false) Integer retryCount
    ) {
        NotificationProcessingResult result = notificationWorkflowService.handleEventForDelivery(new EventNotificationRequest(
                "application-status-changed-" + event.applicationId() + "-" + event.newStatus(),
                NotificationEventType.APPLICATION_STATUS_CHANGED,
                event.studentId(),
                null,
                null,
                event.timestamp(),
                Map.of(
                        "applicationId", String.valueOf(event.applicationId()),
                        "jobId", String.valueOf(event.jobId()),
                        "employerId", event.employerId(),
                        "oldStatus", event.oldStatus(),
                        "newStatus", event.newStatus()
                )
        ));

        handleRecipientWarmupRetry(
                result,
                event,
                retryCount,
                APPLICATION_STATUS_CHANGED_RETRY_ROUTING_KEY
        );
    }

    @RabbitListener(queues = APPLICATION_WITHDRAWN_QUEUE)
    public void onApplicationWithdrawn(
            ApplicationWithdrawnEvent event,
            @Header(name = NotificationRetryPublisher.RETRY_COUNT_HEADER, required = false) Integer retryCount
    ) {
        NotificationProcessingResult result = notificationWorkflowService.handleEventForDelivery(new EventNotificationRequest(
                "application-withdrawn-" + event.applicationId(),
                NotificationEventType.APPLICATION_WITHDRAWN,
                event.studentId(),
                null,
                null,
                event.timestamp(),
                Map.of(
                        "applicationId", String.valueOf(event.applicationId()),
                        "jobId", String.valueOf(event.jobId())
                )
        ));

        handleRecipientWarmupRetry(
                result,
                event,
                retryCount,
                APPLICATION_WITHDRAWN_RETRY_ROUTING_KEY
        );
    }

    @RabbitListener(queues = JOB_POSTED_QUEUE)
    public void onJobPosted(
            JobPostedEvent event,
            @Header(name = NotificationRetryPublisher.RETRY_COUNT_HEADER, required = false) Integer retryCount
    ) {
        NotificationProcessingResult result = notificationWorkflowService.handleEventForDelivery(new EventNotificationRequest(
                "job-posted-" + event.jobId(),
                NotificationEventType.JOB_POSTED,
                event.employerId(),
                null,
                null,
                event.timestamp(),
                Map.of(
                        "jobId", String.valueOf(event.jobId()),
                        "title", event.title()
                )
        ));

        handleRecipientWarmupRetry(
                result,
                event,
                retryCount,
                JOB_POSTED_RETRY_ROUTING_KEY
        );
    }

    @RabbitListener(queues = EMPLOYER_VERIFIED_QUEUE)
    public void onEmployerVerified(
            EmployerVerifiedEvent event,
            @Header(name = NotificationRetryPublisher.RETRY_COUNT_HEADER, required = false) Integer retryCount
    ) {
        NotificationProcessingResult result = notificationWorkflowService.handleEventForDelivery(new EventNotificationRequest(
                "employer-verified-" + event.employerId() + "-" + event.verificationStatus(),
                NotificationEventType.EMPLOYER_VERIFIED,
                event.employerId(),
                null,
                null,
                event.timestamp(),
                Map.of("verificationStatus", event.verificationStatus())
        ));

        handleRecipientWarmupRetry(
                result,
                event,
                retryCount,
                EMPLOYER_VERIFIED_RETRY_ROUTING_KEY
        );
    }

    private void handleRecipientWarmupRetry(
            NotificationProcessingResult result,
            Object event,
            Integer retryCount,
            String retryRoutingKey
    ) {
        if (!result.waitingForRecipient()) {
            return;
        }

        int currentRetryCount = retryCount != null ? retryCount : 0;
        if (currentRetryCount < retryProperties.maxRecipientWarmupRetries()) {
            notificationRetryPublisher.publishDelayedRetry(retryRoutingKey, event, currentRetryCount + 1);
            return;
        }

        notificationWorkflowService.scheduleRecipientRecovery(result.notificationId());
    }
}
