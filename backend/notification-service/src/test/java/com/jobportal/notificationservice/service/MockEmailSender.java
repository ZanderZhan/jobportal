package com.jobportal.notificationservice.service;

class MockEmailSender implements EmailSender {

    private EmailSendResult nextResult = EmailSendResult.success();

    void simulateSuccess() {
        nextResult = EmailSendResult.success();
    }

    void simulateTemporaryFailure(String message) {
        nextResult = EmailSendResult.temporaryFailure(message);
    }

    void simulatePermanentFailure(String message) {
        nextResult = EmailSendResult.permanentFailure(message);
    }

    void simulateTimeout(String message) {
        nextResult = EmailSendResult.timeout(message);
    }

    @Override
    public EmailSendResult send(String email, String subject, String body) {
        return nextResult;
    }
}
