package com.jobportal.notificationservice.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class LoggingEmailSender implements EmailSender {

    private static final Logger log = LoggerFactory.getLogger(LoggingEmailSender.class);

    @Override
    public void send(String email, String subject, String body) {
        // For now this acts as a simple delivery stub instead of a real mail provider.
        if (!StringUtils.hasText(email)) {
            throw new IllegalArgumentException("Recipient email is missing.");
        }

        log.info("Sending email to {} with subject '{}': {}", email, subject, body);
    }
}
