package com.jobportal.notificationservice.service;

public interface EmailSender {

    EmailSendResult send(String email, String subject, String body);
}
