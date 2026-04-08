package com.jobportal.gateway.filter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Base64;

@Component
public class UserContextFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(UserContextFilter.class);
    private static final String X_USER_ID_HEADER = "X-User-Id";
    private static final String X_USER_ROLE_HEADER = "X-User-Role";

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            try {
                String[] parts = token.split("\\.");
                if (parts.length >= 2) {
                    byte[] decodedBytes = Base64.getUrlDecoder().decode(addPadding(parts[1]));
                    JsonNode claims = objectMapper.readTree(decodedBytes);

                    ServerWebExchange.Builder mutatedBuilder = exchange.mutate();
                    String userId = claims.has("sub") ? claims.get("sub").asText() : null;
                    String role = claims.has("role") ? claims.get("role").asText() : null;

                    if (userId != null || role != null) {
                        final String finalUserId = userId;
                        final String finalRole = role;
                        ServerWebExchange mutatedExchange = mutatedBuilder
                            .request(r -> r.headers(headers -> {
                                if (finalUserId != null) {
                                    headers.set(X_USER_ID_HEADER, finalUserId);
                                }
                                if (finalRole != null) {
                                    headers.set(X_USER_ROLE_HEADER, finalRole);
                                }
                            }))
                            .build();
                        return chain.filter(mutatedExchange);
                    }
                }
            } catch (Exception e) {
                log.debug("Could not extract user context from token: {} - {}", e.getClass().getSimpleName(), e.getMessage());
            }
        }
        return chain.filter(exchange);
    }

    private String addPadding(String base64) {
        int remainder = base64.length() % 4;
        if (remainder == 2) return base64 + "==";
        if (remainder == 3) return base64 + "=";
        return base64;
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 1;
    }
}
