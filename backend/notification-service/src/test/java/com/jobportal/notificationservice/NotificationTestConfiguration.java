package com.jobportal.notificationservice;

import com.jobportal.notificationservice.dto.NotificationDispatchEvent;
import com.jobportal.notificationservice.service.EmailSendResult;
import com.jobportal.notificationservice.service.EmailSender;
import com.jobportal.notificationservice.service.NotificationDispatchPublisher;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

@TestConfiguration
public class NotificationTestConfiguration {

    @Bean
    @Primary
    ControllableEmailSender testEmailSender() {
        return new ControllableEmailSender();
    }

    @Bean
    @Primary
    CapturingNotificationDispatchPublisher testNotificationDispatchPublisher() {
        return new CapturingNotificationDispatchPublisher();
    }

    public static class CapturingNotificationDispatchPublisher implements NotificationDispatchPublisher {

        private final List<NotificationDispatchEvent> events = new ArrayList<>();

        @Override
        public void publish(NotificationDispatchEvent event) {
            events.add(event);
        }

        public List<NotificationDispatchEvent> events() {
            return events;
        }

        public void clear() {
            events.clear();
        }
    }

    public static class ControllableEmailSender implements EmailSender {

        private EmailSendResult nextResult = EmailSendResult.success();

        public void simulateSuccess() {
            nextResult = EmailSendResult.success();
        }

        public void simulateTemporaryFailure(String message) {
            nextResult = EmailSendResult.temporaryFailure(message);
        }

        public void simulatePermanentFailure(String message) {
            nextResult = EmailSendResult.permanentFailure(message);
        }

        @Override
        public EmailSendResult send(String email, String subject, String body) {
            if (!StringUtils.hasText(email)) {
                return EmailSendResult.permanentFailure("Recipient email is missing.");
            }
            return nextResult;
        }
    }
}
