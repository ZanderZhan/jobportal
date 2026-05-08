package com.jobportal.notificationservice.controller;

import com.jobportal.notificationservice.NotificationTestConfiguration;
import com.jobportal.notificationservice.dto.EventNotificationRequest;
import com.jobportal.notificationservice.entity.NotificationEventType;
import com.jobportal.notificationservice.repository.NotificationPreferenceRepository;
import com.jobportal.notificationservice.repository.NotificationRepository;
import com.jobportal.notificationservice.service.NotificationWorkflowService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.Instant;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;

@SpringBootTest
@Import(NotificationTestConfiguration.class)
@ActiveProfiles("test")
class NotificationControllerTest {

    @Autowired
    private NotificationWorkflowService workflowService;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private NotificationPreferenceRepository notificationPreferenceRepository;

    private MockMvc mockMvc;

    private Long notificationId;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();
        notificationRepository.deleteAll();
        notificationPreferenceRepository.deleteAll();

        notificationId = workflowService.handleEvent(new EventNotificationRequest(
                "job-posted-88",
                NotificationEventType.JOB_POSTED,
                "employer-9",
                "employer9@example.com",
                "Employer Nine",
                Instant.parse("2026-04-07T22:20:00Z"),
                Map.of(
                        "jobId", "88",
                        "title", "Backend Engineer"
                )
        )).id();
    }

    @Test
    void shouldReturnNotificationsForRecipient() throws Exception {
        mockMvc.perform(get("/api/notifications/me")
                        .with(jwt().jwt(jwt -> jwt
                                .subject("employer-9")
                                .claim("role", "HIRING")
                                .claim("email", "employer9@example.com")
                                .claim("name", "Employer Nine")
                        ))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].recipientUserId").value("employer-9"))
                .andExpect(jsonPath("$.items[0].eventType").value("JOB_POSTED"));
    }

    @Test
    void shouldExposeNotificationSummary() throws Exception {
        mockMvc.perform(get("/api/notifications/summary")
                        .with(jwt().jwt(jwt -> jwt
                                .subject("employer-9")
                                .claim("role", "HIRING")
                        ))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCount").value(1))
                .andExpect(jsonPath("$.unreadCount").value(1))
                .andExpect(jsonPath("$.actionRequiredCount").value(0))
                .andExpect(jsonPath("$.pendingCount").value(1));
    }

    @Test
    void shouldExposeBootstrapResponse() throws Exception {
        mockMvc.perform(get("/api/notifications/bootstrap")
                        .with(jwt().jwt(jwt -> jwt
                                .subject("employer-9")
                                .claim("role", "HIRING")
                        ))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recipientUserId").value("employer-9"))
                .andExpect(jsonPath("$.summary.totalCount").value(1));
    }

    @Test
    void shouldMarkNotificationAsRead() throws Exception {
        mockMvc.perform(patch("/api/notifications/{notificationId}/read", notificationId)
                        .with(jwt().jwt(jwt -> jwt
                                .subject("employer-9")
                                .claim("role", "HIRING")
                        ))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.read").value(true));
    }
}
