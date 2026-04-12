package com.jobportal.profileservice.exception;

public class ProfileServiceException extends RuntimeException {

    private final String errorCode;
    private final int httpStatus;

    public ProfileServiceException(String errorCode, String message, int httpStatus) {
        super(message);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public int getHttpStatus() {
        return httpStatus;
    }
}
