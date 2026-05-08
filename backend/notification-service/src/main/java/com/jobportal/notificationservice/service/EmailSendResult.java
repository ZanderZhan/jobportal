package com.jobportal.notificationservice.service;

public record EmailSendResult(
        EmailSendStatus status,
        String message
) {

    public static EmailSendResult success() {
        return new EmailSendResult(EmailSendStatus.SUCCESS, null);
    }

    public static EmailSendResult temporaryFailure(String message) {
        return new EmailSendResult(EmailSendStatus.TEMPORARY_FAILURE, message);
    }

    public static EmailSendResult permanentFailure(String message) {
        return new EmailSendResult(EmailSendStatus.PERMANENT_FAILURE, message);
    }

    public static EmailSendResult timeout(String message) {
        return new EmailSendResult(EmailSendStatus.TIMEOUT, message);
    }

    public boolean successful() {
        return status == EmailSendStatus.SUCCESS;
    }
}
