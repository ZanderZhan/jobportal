package com.jobportal.applicationservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.jobportal.applicationservice.dto.ApplicationCreateRequest;
import com.jobportal.applicationservice.dto.ApplicationResponse;
import com.jobportal.applicationservice.dto.ApplicationTimelineEntryResponse;
import com.jobportal.applicationservice.entity.ApplicationStatus;
import com.jobportal.applicationservice.exception.GlobalExceptionHandler;
import com.jobportal.applicationservice.service.ApplicationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.endsWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ApplicationController.class)
@Import(GlobalExceptionHandler.class)
class ApplicationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ApplicationService applicationService;

    private ObjectMapper objectMapper;
    private ApplicationCreateRequest createRequest;
    private ApplicationResponse applicationResponse;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        createRequest = new ApplicationCreateRequest(1L, "resume://student-1.pdf");
        applicationResponse = new ApplicationResponse(
                10L,
                "student-1",
                1L,
                "employer-1",
                "Software Engineer",
                "resume://student-1.pdf",
                ApplicationStatus.SUBMITTED,
                LocalDateTime.of(2026, 4, 11, 10, 30),
                LocalDateTime.of(2026, 4, 11, 10, 30),
                List.of(new ApplicationTimelineEntryResponse(
                        100L,
                        null,
                        ApplicationStatus.SUBMITTED,
                        "student-1",
                        "Application submitted",
                        LocalDateTime.of(2026, 4, 11, 10, 30)
                ))
        );
    }

    @Test
    void submitApplication_WithValidRequest_ShouldReturnCreated() throws Exception {
        when(applicationService.submitApplication(eq("student-1"), eq("JOB_SEEKER"), any(ApplicationCreateRequest.class)))
                .thenReturn(applicationResponse);

                mockMvc.perform(post("/api/applications")
                        .header("X-User-Id", "student-1")
                        .header("X-User-Role", "JOB_SEEKER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", endsWith("/api/applications/10")))
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.jobId").value(1))
                .andExpect(jsonPath("$.status").value("SUBMITTED"))
                .andExpect(jsonPath("$.timeline[0].newStatus").value("SUBMITTED"));
    }

    @Test
    void submitApplication_WithInvalidRequest_ShouldReturnBadRequest() throws Exception {
        ApplicationCreateRequest invalidRequest = new ApplicationCreateRequest(null, " ");

        mockMvc.perform(post("/api/applications")
                        .header("X-User-Id", "student-1")
                        .header("X-User-Role", "JOB_SEEKER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors.jobId").value("Job ID is required"))
                .andExpect(jsonPath("$.errors.resumeReference").value("Resume reference is required"));
    }

    @Test
    void getStudentApplications_ShouldReturnApplications() throws Exception {
        when(applicationService.getStudentApplications("student-1", "JOB_SEEKER"))
                .thenReturn(List.of(applicationResponse));

        mockMvc.perform(get("/api/applications")
                        .header("X-User-Id", "student-1")
                        .header("X-User-Role", "JOB_SEEKER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(10))
                .andExpect(jsonPath("$[0].status").value("SUBMITTED"));
    }

    @Test
    void getStudentApplicationById_ShouldReturnApplication() throws Exception {
        when(applicationService.getStudentApplicationById(10L, "student-1", "JOB_SEEKER"))
                .thenReturn(applicationResponse);

        mockMvc.perform(get("/api/applications/10")
                        .header("X-User-Id", "student-1")
                        .header("X-User-Role", "JOB_SEEKER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.jobTitle").value("Software Engineer"));
    }

    @Test
    void withdrawApplication_ShouldReturnUpdatedApplication() throws Exception {
        ApplicationResponse withdrawnResponse = new ApplicationResponse(
                10L,
                "student-1",
                1L,
                "employer-1",
                "Software Engineer",
                "resume://student-1.pdf",
                ApplicationStatus.WITHDRAWN,
                LocalDateTime.of(2026, 4, 11, 10, 30),
                LocalDateTime.of(2026, 4, 12, 9, 0),
                List.of(
                        new ApplicationTimelineEntryResponse(
                                100L,
                                null,
                                ApplicationStatus.SUBMITTED,
                                "student-1",
                                "Application submitted",
                                LocalDateTime.of(2026, 4, 11, 10, 30)
                        ),
                        new ApplicationTimelineEntryResponse(
                                101L,
                                ApplicationStatus.SUBMITTED,
                                ApplicationStatus.WITHDRAWN,
                                "student-1",
                                "Application withdrawn by student",
                                LocalDateTime.of(2026, 4, 12, 9, 0)
                        )
                )
        );

        when(applicationService.withdrawApplication(10L, "student-1", "JOB_SEEKER"))
                .thenReturn(withdrawnResponse);

        mockMvc.perform(put("/api/applications/10/withdraw")
                        .header("X-User-Id", "student-1")
                        .header("X-User-Role", "JOB_SEEKER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("WITHDRAWN"))
                .andExpect(jsonPath("$.timeline[1].newStatus").value("WITHDRAWN"));
    }
}
