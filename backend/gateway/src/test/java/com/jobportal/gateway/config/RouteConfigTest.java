package com.jobportal.gateway.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class RouteConfigTest {

    @Autowired
    private RouteLocator routeLocator;

    @Test
    void shouldHaveJobServiceRoute() {
        var routes = routeLocator.getRoutes().collectList().block();

        assertThat(routes).isNotNull();
        assertThat(routes).hasSizeGreaterThan(0);

        // Verify job-service route exists
        boolean hasJobRoute = routes.stream()
            .anyMatch(route -> route.getId().equals("job-service"));
        assertThat(hasJobRoute).isTrue();
    }

    @Test
    void shouldRouteApiJobsRequestsToJobService() {
        var routes = routeLocator.getRoutes().collectList().block();

        assertThat(routes).isNotNull();
        // Verify at least one job-service route is configured for path /api/jobs/**
        boolean hasJobsApiRoute = routes.stream()
            .anyMatch(route ->
                "job-service".equals(route.getId())
                    && route.getPredicate() != null
                    && route.getPredicate().toString().contains("/api/jobs/**")
            );
        assertThat(hasJobsApiRoute).isTrue();
    }

    @Test
    void shouldHaveApplicationServiceRoute() {
        var routes = routeLocator.getRoutes().collectList().block();

        assertThat(routes).isNotNull();

        boolean hasApplicationRoute = routes.stream()
            .anyMatch(route -> route.getId().equals("application-service"));
        assertThat(hasApplicationRoute).isTrue();
    }

    @Test
    void shouldRouteApiApplicationsRequestsToApplicationService() {
        var routes = routeLocator.getRoutes().collectList().block();

        assertThat(routes).isNotNull();

        boolean hasApplicationsApiRoute = routes.stream()
            .anyMatch(route ->
                "application-service".equals(route.getId())
                    && route.getPredicate() != null
                    && route.getPredicate().toString().contains("/api/applications/**")
            );
        assertThat(hasApplicationsApiRoute).isTrue();
    }

    @Test
    void shouldHaveProfileServiceRoute() {
        var routes = routeLocator.getRoutes().collectList().block();

        assertThat(routes).isNotNull();

        boolean hasProfileRoute = routes.stream()
            .anyMatch(route -> route.getId().equals("profile-service"));
        assertThat(hasProfileRoute).isTrue();
    }

    @Test
    void shouldRouteApiProfilesRequestsToProfileService() {
        var routes = routeLocator.getRoutes().collectList().block();

        assertThat(routes).isNotNull();

        boolean hasProfilesApiRoute = routes.stream()
            .anyMatch(route ->
                "profile-service".equals(route.getId())
                    && route.getPredicate() != null
                    && route.getPredicate().toString().contains("/api/profiles/**")
            );
        assertThat(hasProfilesApiRoute).isTrue();
    }

    @Test
    void shouldHaveNotificationServiceRoute() {
        var routes = routeLocator.getRoutes().collectList().block();

        assertThat(routes).isNotNull();

        boolean hasNotificationRoute = routes.stream()
            .anyMatch(route -> route.getId().startsWith("notification-service"));
        assertThat(hasNotificationRoute).isTrue();
    }

    @Test
    void shouldRouteApiNotificationsRequestsToNotificationService() {
        var routes = routeLocator.getRoutes().collectList().block();

        assertThat(routes).isNotNull();

        boolean hasNotificationsApiRoute = routes.stream()
            .anyMatch(route ->
                "notification-service-user".equals(route.getId())
                    && route.getPredicate() != null
                    && route.getPredicate().toString().contains("/api/notifications/**")
            );
        assertThat(hasNotificationsApiRoute).isTrue();
    }
}
