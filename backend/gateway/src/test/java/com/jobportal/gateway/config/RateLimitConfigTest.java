package com.jobportal.gateway.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class RateLimitConfigTest {

    @Value("${gateway.ratelimit.requests-per-minute:100}")
    private int requestsPerMinute;

    @Value("${gateway.ratelimit.auth-requests-per-minute:10}")
    private int authRequestsPerMinute;

    @Test
    void rateLimitShouldBeConfigured() {
        // Explicitly set to 200 in test profile
        assertThat(requestsPerMinute).isEqualTo(200);
    }

    @Test
    void authRateLimitShouldBeStricter() {
        assertThat(authRequestsPerMinute).isLessThan(requestsPerMinute);
        assertThat(authRequestsPerMinute).isEqualTo(5);
    }
}
