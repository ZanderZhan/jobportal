package com.jobportal.notificationservice.controller;

import com.jobportal.notificationservice.dto.EventNotificationRequest;
import com.jobportal.notificationservice.entity.NotificationEventType;
import com.jobportal.notificationservice.service.NotificationWorkflowService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
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

@SpringBootTest
@ActiveProfiles("test")
class NotificationControllerTest {

    @Autowired
    private NotificationWorkflowService workflowService;

    @Autowired
    private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc;

    private Long notificationId;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();

        notificationId = workflowService.handleEvent(new EventNotificationRequest(
                "job-posted-88",
                NotificationEventType.JOB_POSTED,
                9L,
                null,
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
                        .param("recipientUserId", "9")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].recipientUserId").value(9))
                .andExpect(jsonPath("$[0].eventType").value("JOB_POSTED"));
    }

    @Test
    void shouldMarkNotificationAsRead() throws Exception {
        mockMvc.perform(patch("/api/notifications/{notificationId}/read", notificationId)
                        .param("recipientUserId", "9")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.read").value(true));
    }
}
