package com.jobportal.authservice.dto;

import jakarta.validation.constraints.NotBlank;

public class OAuthCallbackRequest {

    @NotBlank(message = "Code is required")
    private String code;

    @NotBlank(message = "State is required")
    private String state;

    private String redirectUri;

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getRedirectUri() {
        return redirectUri;
    }

    public void setRedirectUri(String redirectUri) {
        this.redirectUri = redirectUri;
    }
}
