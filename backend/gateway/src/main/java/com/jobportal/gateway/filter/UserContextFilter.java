package com.jobportal.gateway.filter;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Locale;

@Component
public class UserContextFilter implements GlobalFilter, Ordered {

    private static final String X_USER_ID_HEADER = "X-User-Id";
    private static final String X_USER_ROLE_HEADER = "X-User-Role";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        return exchange.getPrincipal()
                .ofType(Authentication.class)
                .filter(Authentication::isAuthenticated)
                .flatMap(authentication -> {
                    String userId = resolveUserId(authentication);
                    String role = resolveRole(authentication);
                    if (!StringUtils.hasText(userId) && !StringUtils.hasText(role)) {
                        return chain.filter(exchange);
                    }

                    ServerWebExchange mutatedExchange = exchange.mutate()
                            .request(request -> request.headers(headers -> {
                                if (StringUtils.hasText(userId)) {
                                    headers.set(X_USER_ID_HEADER, userId);
                                }
                                if (StringUtils.hasText(role)) {
                                    headers.set(X_USER_ROLE_HEADER, role);
                                }
                            }))
                            .build();

                    return chain.filter(mutatedExchange);
                })
                .switchIfEmpty(chain.filter(exchange));
    }

    private String resolveUserId(Authentication authentication) {
        if (authentication instanceof JwtAuthenticationToken jwtAuthenticationToken) {
            return jwtAuthenticationToken.getToken().getSubject();
        }
        return authentication.getName();
    }

    private String resolveRole(Authentication authentication) {
        if (authentication instanceof JwtAuthenticationToken jwtAuthenticationToken) {
            String roleClaim = jwtAuthenticationToken.getToken().getClaimAsString("role");
            if (StringUtils.hasText(roleClaim)) {
                return roleClaim.toUpperCase(Locale.ROOT);
            }
        }

        for (GrantedAuthority authority : authentication.getAuthorities()) {
            String value = authority.getAuthority();
            if (StringUtils.hasText(value) && value.startsWith("ROLE_")) {
                return value.substring("ROLE_".length()).toUpperCase(Locale.ROOT);
            }
        }

        return null;
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 1;
    }
}
