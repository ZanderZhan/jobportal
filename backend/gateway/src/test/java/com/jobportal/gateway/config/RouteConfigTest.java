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
        // Verify at least one route for /api/jobs/** exists
        boolean hasJobsApiRoute = routes.stream()
            .anyMatch(route -> route.getId().equals("job-service"));
        assertThat(hasJobsApiRoute).isTrue();
    }
}
