package com.jobportal.notificationservice.service;

public interface EmailSender {

    void send(String email, String subject, String body);
}
