package com.jobportal.notificationservice.config;

import com.jobportal.notificationservice.entity.DeliveryChannel;
import com.jobportal.notificationservice.entity.NotificationEventType;
import com.jobportal.notificationservice.entity.NotificationTemplate;
import com.jobportal.notificationservice.repository.NotificationTemplateRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class NotificationSeedConfiguration {

    @Bean
    CommandLineRunner seedTemplates(NotificationTemplateRepository templateRepository) {
        return args -> {
            // A few default templates make the service usable on first startup.
            saveIfMissing(
                    templateRepository,
                    template(
                            NotificationEventType.APPLICATION_SUBMITTED,
                            DeliveryChannel.IN_APP,
                            "Application received",
                            "Hi #{recipientName}, your application for job #{jobId} has been submitted."
                    )
            );
            saveIfMissing(
                    templateRepository,
                    template(
                            NotificationEventType.APPLICATION_SUBMITTED,
                            DeliveryChannel.EMAIL,
                            "Application received",
                            "Hi #{recipientName}, we received your application for job #{jobId}."
                    )
            );
            saveIfMissing(
                    templateRepository,
                    template(
                            NotificationEventType.APPLICATION_STATUS_CHANGED,
                            DeliveryChannel.IN_APP,
                            "Application status updated",
                            "Hi #{recipientName}, your application moved from #{oldStatus} to #{newStatus}."
                    )
            );
            saveIfMissing(
                    templateRepository,
                    template(
                            NotificationEventType.APPLICATION_STATUS_CHANGED,
                            DeliveryChannel.EMAIL,
                            "Application status updated",
                            "Hi #{recipientName}, your application status is now #{newStatus}."
                    )
            );
            saveIfMissing(
                    templateRepository,
                    template(
                            NotificationEventType.APPLICATION_WITHDRAWN,
                            DeliveryChannel.IN_APP,
                            "Application withdrawn",
                            "Your application for job #{jobId} has been withdrawn."
                    )
            );
            saveIfMissing(
                    templateRepository,
                    template(
                            NotificationEventType.APPLICATION_WITHDRAWN,
                            DeliveryChannel.EMAIL,
                            "Application withdrawn",
                            "Your application for job #{jobId} was withdrawn successfully."
                    )
            );
            saveIfMissing(
                    templateRepository,
                    template(
                            NotificationEventType.JOB_POSTED,
                            DeliveryChannel.IN_APP,
                            "Job posted",
                            "Your job '#{title}' is now published."
                    )
            );
            saveIfMissing(
                    templateRepository,
                    template(
                            NotificationEventType.JOB_POSTED,
                            DeliveryChannel.EMAIL,
                            "Job posted",
                            "Your job '#{title}' is now published and ready to receive applications."
                    )
            );
            saveIfMissing(
                    templateRepository,
                    template(
                            NotificationEventType.EMPLOYER_VERIFIED,
                            DeliveryChannel.IN_APP,
                            "Employer verified",
                            "Your employer account is now #{verificationStatus}."
                    )
            );
            saveIfMissing(
                    templateRepository,
                    template(
                            NotificationEventType.EMPLOYER_VERIFIED,
                            DeliveryChannel.EMAIL,
                            "Employer verified",
                            "Your employer account is now #{verificationStatus}."
                    )
            );
        };
    }

    private void saveIfMissing(NotificationTemplateRepository templateRepository, NotificationTemplate template) {
        boolean exists = templateRepository.findByEventTypeAndChannelAndActiveTrue(template.getEventType(), template.getChannel()).isPresent();
        if (!exists) {
            templateRepository.save(template);
        }
    }

    private NotificationTemplate template(
            NotificationEventType eventType,
            DeliveryChannel channel,
            String subjectTemplate,
            String bodyTemplate
    ) {
        NotificationTemplate template = new NotificationTemplate();
        template.setEventType(eventType);
        template.setChannel(channel);
        template.setSubjectTemplate(subjectTemplate);
        template.setBodyTemplate(bodyTemplate);
        template.setActive(true);
        return template;
    }
}
