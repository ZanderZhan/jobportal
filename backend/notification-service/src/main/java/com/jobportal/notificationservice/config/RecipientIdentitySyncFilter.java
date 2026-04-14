package com.jobportal.notificationservice.config;

import com.jobportal.notificationservice.service.RecipientIdentityService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

public class RecipientIdentitySyncFilter extends OncePerRequestFilter {

    private final RecipientIdentityService recipientIdentityService;

    public RecipientIdentitySyncFilter(RecipientIdentityService recipientIdentityService) {
        this.recipientIdentityService = recipientIdentityService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication instanceof JwtAuthenticationToken jwtAuthenticationToken && authentication.isAuthenticated()) {
            Jwt jwt = jwtAuthenticationToken.getToken();
            String userId = jwt.getSubject();

            if (StringUtils.hasText(userId)) {
                recipientIdentityService.remember(
                        userId,
                        jwt.getClaimAsString("email"),
                        jwt.getClaimAsString("name"),
                        jwt.getClaimAsString("role"),
                        "http-jwt"
                );
            }
        }

        filterChain.doFilter(request, response);
    }
}
