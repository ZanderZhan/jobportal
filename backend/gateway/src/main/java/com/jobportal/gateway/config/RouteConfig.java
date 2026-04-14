package com.jobportal.gateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RouteConfig {

    @Value("${services.job-service.url:http://localhost:8081}")
    private String jobServiceUrl;

    @Value("${services.auth-service.url:http://localhost:8082}")
    private String authServiceUrl;

    @Value("${services.application-service.url:http://localhost:8084}")
    private String applicationServiceUrl;

    @Value("${services.profile-service.url:http://localhost:8085}")
    private String profileServiceUrl;

    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
            .route("job-service", r -> r
                .path("/api/jobs/**")
                .uri(jobServiceUrl))
            .route("auth-service", r -> r
                .path("/api/auth/**")
                .uri(authServiceUrl))
            .route("application-service", r -> r
                .path("/api/applications/**")
                .uri(applicationServiceUrl))
            .route("profile-service", r -> r
                .path("/api/profiles/**")
                .uri(profileServiceUrl))
            .build();
    }
}
