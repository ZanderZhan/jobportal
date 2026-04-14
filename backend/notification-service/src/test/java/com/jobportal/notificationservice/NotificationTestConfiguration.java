package com.jobportal.notificationservice;

import com.jobportal.notificationservice.service.EmailSender;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.util.StringUtils;

@TestConfiguration
public class NotificationTestConfiguration {

    @Bean
    @Primary
    EmailSender testEmailSender() {
        return (email, subject, body) -> {
            if (!StringUtils.hasText(email)) {
                throw new IllegalArgumentException("Recipient email is missing.");
            }
        };
    }
}
