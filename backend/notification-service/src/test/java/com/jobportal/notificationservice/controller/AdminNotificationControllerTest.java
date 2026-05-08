package com.jobportal.notificationservice.controller;

import com.jobportal.notificationservice.NotificationTestConfiguration;
import com.jobportal.notificationservice.entity.Notification;
import com.jobportal.notificationservice.entity.NotificationEventType;
import com.jobportal.notificationservice.entity.NotificationStatus;
import com.jobportal.notificationservice.repository.NotificationPreferenceRepository;
import com.jobportal.notificationservice.repository.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.Instant;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@Import(NotificationTestConfiguration.class)
@ActiveProfiles("test")
class AdminNotificationControllerTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private NotificationPreferenceRepository notificationPreferenceRepository;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();
        notificationRepository.deleteAll();
        notificationPreferenceRepository.deleteAll();
    }

    @Test
    void shouldRejectStandardUserFromFailedNotificationsEndpoint() throws Exception {
        mockMvc.perform(get("/api/admin/notifications/failed")
                        .with(jwt().jwt(jwt -> jwt
                                        .subject("student-1")
                                        .claim("role", "STUDENT"))
                                .authorities(new SimpleGrantedAuthority("ROLE_STUDENT")))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldRejectStandardUserFromRetryEndpoint() throws Exception {
        Long notificationId = notificationRepository.save(notification(NotificationStatus.FAILED)).getId();

        mockMvc.perform(post("/api/admin/notifications/{id}/retry", notificationId)
                        .with(jwt().jwt(jwt -> jwt
                                        .subject("student-1")
                                        .claim("role", "STUDENT"))
                                .authorities(new SimpleGrantedAuthority("ROLE_STUDENT")))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldAllowAdminToQueryFailedNotifications() throws Exception {
        notificationRepository.save(notification(NotificationStatus.FAILED));

        mockMvc.perform(get("/api/admin/notifications/failed")
                        .with(adminJwt())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].status").value("FAILED"));
    }

    @Test
    void shouldAllowAdminToRetryFailedNotificationSynchronously() throws Exception {
        Long notificationId = notificationRepository.save(notification(NotificationStatus.FAILED)).getId();

        mockMvc.perform(post("/api/admin/notifications/{id}/retry", notificationId)
                        .with(adminJwt())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SENT"));
    }

    @Test
    void shouldRejectAdminRetryForSentNotification() throws Exception {
        Long notificationId = notificationRepository.save(notification(NotificationStatus.SENT)).getId();

        mockMvc.perform(post("/api/admin/notifications/{id}/retry", notificationId)
                        .with(adminJwt())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    private org.springframework.test.web.servlet.request.RequestPostProcessor adminJwt() {
        return jwt().jwt(jwt -> jwt
                        .subject("admin-1")
                        .claim("role", "ADMIN"))
                .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"));
    }

    private Notification notification(NotificationStatus status) {
        Notification notification = new Notification();
        notification.setEventKey("event-" + status + "-" + System.nanoTime());
        notification.setEventType(NotificationEventType.APPLICATION_SUBMITTED);
        notification.setRecipientUserId("student-1");
        notification.setRecipientEmail("student@example.com");
        notification.setRecipientName("Student One");
        notification.setTitle("Application received");
        notification.setBody("Your application was received.");
        notification.setEmailSubject("Application received");
        notification.setEmailBody("Your application was received.");
        notification.setStatus(status);
        notification.setRead(false);
        notification.setCreatedAt(Instant.parse("2026-04-07T22:00:00Z"));
        if (status == NotificationStatus.FAILED) {
            notification.setRetryCount(1);
            notification.setFailureReason("provider unavailable");
        }
        return notification;
    }
}
