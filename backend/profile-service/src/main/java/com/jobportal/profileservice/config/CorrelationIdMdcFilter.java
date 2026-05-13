package com.jobportal.profileservice.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class CorrelationIdMdcFilter extends OncePerRequestFilter {

    public static final String MDC_KEY = "correlationId";
    private static final String LEGACY_CORRELATION_HEADER = "X-Correlation-Id";

    private final String correlationIdHeader;

    public CorrelationIdMdcFilter(
            @Value("${profile.correlation-id-header:X-Request-ID}") String correlationIdHeader) {
        this.correlationIdHeader = correlationIdHeader;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        String correlationId = firstHeaderValue(request, correlationIdHeader);
        if (correlationId == null && !LEGACY_CORRELATION_HEADER.equalsIgnoreCase(correlationIdHeader)) {
            correlationId = firstHeaderValue(request, LEGACY_CORRELATION_HEADER);
        }

        if (correlationId != null) {
            MDC.put(MDC_KEY, correlationId);
        }

        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(MDC_KEY);
        }
    }

    private String firstHeaderValue(HttpServletRequest request, String headerName) {
        String value = request.getHeader(headerName);
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
