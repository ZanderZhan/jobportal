package com.jobportal.notificationservice.config;

import com.jobportal.notificationservice.entity.DeliveryChannel;
import com.jobportal.notificationservice.entity.NotificationEventType;
import com.jobportal.notificationservice.entity.NotificationTemplate;
import com.jobportal.notificationservice.repository.NotificationTemplateRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class NotificationSeedConfiguration {

    @Bean
    CommandLineRunner seedTemplates(NotificationTemplateRepository templateRepository) {
        return args -> {
            if (templateRepository.count() > 0) {
                return;
            }

            // A few default templates make the service usable on first startup.
            templateRepository.saveAll(List.of(
                    template(
                            NotificationEventType.APPLICATION_SUBMITTED,
                            DeliveryChannel.IN_APP,
                            "Application received",
                            "Your application for job #{jobId} has been submitted."
                    ),
                    template(
                            NotificationEventType.APPLICATION_SUBMITTED,
                            DeliveryChannel.EMAIL,
                            "Application received",
                            "We received your application for job #{jobId}."
                    ),
                    template(
                            NotificationEventType.APPLICATION_STATUS_CHANGED,
                            DeliveryChannel.IN_APP,
                            "Application status updated",
                            "Your application moved from #{oldStatus} to #{newStatus}."
                    ),
                    template(
                            NotificationEventType.APPLICATION_STATUS_CHANGED,
                            DeliveryChannel.EMAIL,
                            "Application status updated",
                            "Your application status is now #{newStatus}."
                    ),
                    template(
                            NotificationEventType.JOB_POSTED,
                            DeliveryChannel.IN_APP,
                            "Job posted",
                            "Job '#{title}' is now published."
                    ),
                    template(
                            NotificationEventType.EMPLOYER_VERIFIED,
                            DeliveryChannel.IN_APP,
                            "Employer verified",
                            "Your employer account is now #{verificationStatus}."
                    )
            ));
        };
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
