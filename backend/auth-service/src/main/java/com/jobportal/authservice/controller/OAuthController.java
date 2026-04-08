package com.jobportal.authservice.controller;

import com.jobportal.authservice.dto.OAuthCallbackRequest;
import com.jobportal.authservice.dto.TokenResponse;
import com.jobportal.authservice.service.OAuthService;
import com.jobportal.authservice.service.OAuthStateService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class OAuthController {

    private final OAuthService oAuthService;
    private final OAuthStateService oAuthStateService;

    public OAuthController(OAuthService oAuthService, OAuthStateService oAuthStateService) {
        this.oAuthService = oAuthService;
        this.oAuthStateService = oAuthStateService;
    }

    @GetMapping("/google/authorize")
    public ResponseEntity<Map<String, String>> getGoogleAuthorizationUrl(
            @RequestParam(required = false) String redirectUri) {
        OAuthStateService.StateData stateData = oAuthStateService.generateState("google", redirectUri);
        String url = oAuthService.getGoogleAuthorizationUrl(stateData.state(), stateData.codeChallenge());
        return ResponseEntity.ok(Map.of("url", url));
    }

    @GetMapping("/google/callback")
    public ResponseEntity<TokenResponse> handleGoogleCallback(
            @RequestParam String code,
            @RequestParam String state,
            HttpServletRequest httpRequest) {
        String userAgent = httpRequest.getHeader("User-Agent");
        String ip = getClientIp(httpRequest);
        OAuthCallbackRequest request = new OAuthCallbackRequest(code, state, null);
        TokenResponse response = oAuthService.handleGoogleCallback(request, userAgent, ip);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/microsoft/authorize")
    public ResponseEntity<Map<String, String>> getMicrosoftAuthorizationUrl(
            @RequestParam(required = false) String redirectUri) {
        OAuthStateService.StateData stateData = oAuthStateService.generateState("microsoft", redirectUri);
        String url = oAuthService.getMicrosoftAuthorizationUrl(stateData.state(), stateData.codeChallenge());
        return ResponseEntity.ok(Map.of("url", url));
    }

    @GetMapping("/microsoft/callback")
    public ResponseEntity<TokenResponse> handleMicrosoftCallback(
            @RequestParam String code,
            @RequestParam(required = false) String state,
            @RequestParam(required = false) String redirectUri,
            HttpServletRequest httpRequest) {
        String userAgent = httpRequest.getHeader("User-Agent");
        String ip = getClientIp(httpRequest);
        OAuthCallbackRequest request = new OAuthCallbackRequest(code, state, redirectUri);
        TokenResponse response = oAuthService.handleMicrosoftCallback(request, userAgent, ip);
        return ResponseEntity.ok(response);
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        return request.getRemoteAddr();
    }
}
