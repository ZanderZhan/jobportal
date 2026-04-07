package com.jobportal.authservice.controller;

import com.jobportal.authservice.dto.*;
import com.jobportal.authservice.exception.AuthException;
import com.jobportal.authservice.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/api/auth/register")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
        UserResponse response = authService.register(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/api/auth/login")
    public ResponseEntity<TokenResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest) {
        String userAgent = httpRequest.getHeader("User-Agent");
        String ip = getClientIp(httpRequest);
        TokenResponse response = authService.login(request, userAgent, ip);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/api/auth/refresh")
    public ResponseEntity<TokenResponse> refresh(
            @CookieValue(name = "refresh_token", required = false) String refreshToken,
            HttpServletRequest httpRequest) {
        if (refreshToken == null || refreshToken.isEmpty()) {
            refreshToken = httpRequest.getHeader("X-Refresh-Token");
        }
        String userAgent = httpRequest.getHeader("User-Agent");
        String ip = getClientIp(httpRequest);
        TokenResponse response = authService.refreshToken(refreshToken, userAgent, ip);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/api/auth/logout")
    public ResponseEntity<Void> logout(
            @CookieValue(name = "refresh_token", required = false) String refreshToken,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            HttpServletRequest httpRequest) {
        if (refreshToken == null || refreshToken.isEmpty()) {
            refreshToken = httpRequest.getHeader("X-Refresh-Token");
        }

        String accessToken = null;
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            accessToken = authHeader.substring(7);
        }

        authService.logout(refreshToken, accessToken);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/api/auth/me")
    public ResponseEntity<UserResponse> getCurrentUser(
            @RequestHeader("Authorization") String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new AuthException("AUTH_TOKEN_NOT_FOUND", "Authorization token not found", 401);
        }
        String token = authHeader.substring(7);
        UserResponse response = authService.getCurrentUser(token);
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
