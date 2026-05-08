package com.jobportal.notificationservice.config;

import com.jobportal.notificationservice.service.RecipientIdentityService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Locale;

@Configuration
public class NotificationSecurityConfiguration {

    @Bean
    SecurityFilterChain notificationSecurityFilterChain(
            HttpSecurity http,
            RecipientIdentitySyncFilter recipientIdentitySyncFilter
    ) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                        .requestMatchers("/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        .requestMatchers("/api/admin/notifications/**").hasAnyRole("ADMIN", "OPERATOR")
                        .anyRequest().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())))
                // Recipient cache sync runs only after token validation has succeeded.
                .addFilterAfter(recipientIdentitySyncFilter, BearerTokenAuthenticationFilter.class)
                .build();
    }

    @Bean
    RecipientIdentitySyncFilter recipientIdentitySyncFilter(RecipientIdentityService recipientIdentityService) {
        return new RecipientIdentitySyncFilter(recipientIdentityService);
    }

    @Bean
    JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            String role = jwt.getClaimAsString("role");
            if (!StringUtils.hasText(role)) {
                return List.of();
            }
            return List.of(new SimpleGrantedAuthority("ROLE_" + role.toUpperCase(Locale.ROOT)));
        });
        return converter;
    }
}
