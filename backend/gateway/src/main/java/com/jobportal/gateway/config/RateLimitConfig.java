package com.jobportal.gateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.filter.ratelimit.RateLimiter;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;
import reactor.core.publisher.Mono;

import jakarta.validation.constraints.NotNull;

@Configuration
@Validated
public class RateLimitConfig {

    @Value("${gateway.ratelimit.requests-per-minute:100}")
    private int requestsPerMinute;

    @Value("${gateway.ratelimit.auth-requests-per-minute:10}")
    private int authRequestsPerMinute;

    @Bean
    public RouteLocator rateLimitedRoutes(RouteLocatorBuilder builder) {
        return builder.routes().build();
    }

    @Bean
    public KeyResolver remoteAddrKeyResolver() {
        return exchange -> {
            String remoteAddr = exchange.getRequest().getRemoteAddress() != null
                ? exchange.getRequest().getRemoteAddress().getAddress().getHostAddress()
                : "unknown";
            return Mono.just(remoteAddr);
        };
    }

    public int getRequestsPerMinute() {
        return requestsPerMinute;
    }

    public int getAuthRequestsPerMinute() {
        return authRequestsPerMinute;
    }
}
