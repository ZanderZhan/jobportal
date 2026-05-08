package com.jobportal.notificationservice.service;

import com.jobportal.notificationservice.entity.Notification;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.Locale;

@Service
public class NotificationAccessService {

    public String requireUserId(String requestedUserId) {
        Jwt jwt = requireJwt();
        String tokenUserId = jwt.getSubject();

        if (!StringUtils.hasText(tokenUserId)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token subject is missing.");
        }

        if (StringUtils.hasText(requestedUserId) && !tokenUserId.equals(requestedUserId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "User context does not match token subject.");
        }

        return tokenUserId;
    }

    public String requireRole(String requestedRole) {
        Jwt jwt = requireJwt();
        String role = jwt.getClaimAsString("role");

        if (!StringUtils.hasText(role)) {
            role = readRoleFromAuthorities();
        }

        if (!StringUtils.hasText(role)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Role is missing in the authenticated token.");
        }

        if (StringUtils.hasText(requestedRole) && !role.equalsIgnoreCase(requestedRole)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Role context does not match token role.");
        }

        return role;
    }

    public void requireAdmin(String role) {
        if (!isAdminOrOperator(role)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "This action is limited to admins or operators.");
        }
    }

    public void ensureOwnerOrAdmin(Notification notification, String userId, String role) {
        if (isAdmin(role)) {
            return;
        }

        if (!notification.getRecipientUserId().equals(requireUserId(userId))) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Notification not found.");
        }
    }

    public boolean isAdmin(String role) {
        return "ADMIN".equalsIgnoreCase(requireRole(role));
    }

    public boolean isAdminOrOperator(String role) {
        String resolvedRole = requireRole(role);
        return "ADMIN".equalsIgnoreCase(resolvedRole) || "OPERATOR".equalsIgnoreCase(resolvedRole);
    }

    private Jwt requireJwt() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!(authentication instanceof JwtAuthenticationToken jwtAuthenticationToken) || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing authenticated token context.");
        }
        return jwtAuthenticationToken.getToken();
    }

    private String readRoleFromAuthorities() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getAuthorities() == null) {
            return null;
        }

        for (GrantedAuthority authority : authentication.getAuthorities()) {
            String value = authority.getAuthority();
            if (StringUtils.hasText(value) && value.startsWith("ROLE_")) {
                return value.substring("ROLE_".length()).toUpperCase(Locale.ROOT);
            }
        }

        return null;
    }
}
