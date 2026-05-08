package com.jobportal.notificationservice.service;

import com.jobportal.notificationservice.config.NotificationMailProperties;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.mail.MailException;
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
            return EmailSendResult.permanentFailure("Recipient email is missing.");
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
        } catch (MailException ex) {
            return EmailSendResult.temporaryFailure(ex.getMessage());
        } catch (MessagingException | UnsupportedEncodingException ex) {
            return EmailSendResult.permanentFailure("Could not build the email message.");
        }
    }
}
