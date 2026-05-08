package com.jobportal.notificationservice.service;

import com.jobportal.notificationservice.config.NotificationMailProperties;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.io.UnsupportedEncodingException;

@Component
public class JavaMailEmailSender implements EmailSender {

    private final JavaMailSender javaMailSender;
    private final NotificationMailProperties notificationMailProperties;

    public JavaMailEmailSender(JavaMailSender javaMailSender, NotificationMailProperties notificationMailProperties) {
        this.javaMailSender = javaMailSender;
        this.notificationMailProperties = notificationMailProperties;
    }

    @Override
    public EmailSendResult send(String email, String subject, String body) {
        if (!StringUtils.hasText(email)) {
            throw new IllegalArgumentException("Recipient email is missing.");
        }

        try {
            MimeMessage message = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, StandardCharsets.UTF_8.name());
            helper.setTo(email);
            helper.setFrom(notificationMailProperties.fromAddress(), notificationMailProperties.fromName());
            helper.setSubject(subject);
            helper.setText(body, false);
            javaMailSender.send(message);
            return EmailSendResult.success();
        } catch (MessagingException | UnsupportedEncodingException ex) {
            throw new IllegalStateException("Could not build the email message.", ex);
        }
    }
}
