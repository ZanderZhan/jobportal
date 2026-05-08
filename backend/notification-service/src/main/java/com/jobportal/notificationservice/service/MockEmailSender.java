package com.jobportal.notificationservice.service;

public class MockEmailSender implements EmailSender {

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

    public void simulateTimeout(String message) {
        nextResult = EmailSendResult.timeout(message);
    }

    @Override
    public EmailSendResult send(String email, String subject, String body) {
        return nextResult;
    }
}
