package com.jobportal.applicationservice;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobportal.applicationservice.dto.ApplicationCreateRequest;
import com.jobportal.applicationservice.dto.ApplicationStatusUpdateRequest;
import com.jobportal.applicationservice.entity.Application;
import com.jobportal.applicationservice.entity.ApplicationStatus;
import com.jobportal.applicationservice.repository.ApplicationRepository;
import com.jobportal.applicationservice.service.ApplicationEligibilityService;
import com.jobportal.applicationservice.service.ApplicationEventPublisher;
import com.jobportal.applicationservice.service.JobDetailsResponse;
import com.jobportal.applicationservice.service.JobEligibility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ApplicationWorkflowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private ObjectMapper objectMapper;

    @Autowired
    private ApplicationRepository applicationRepository;

    @MockitoBean
    private ApplicationEligibilityService applicationEligibilityService;

    @MockitoBean
    private ApplicationEventPublisher applicationEventPublisher;

    @BeforeEach
    void setUp() {
        applicationRepository.deleteAll();
        objectMapper = new ObjectMapper().findAndRegisterModules();
    }

    @Test
    void submitApplication_ShouldPersistApplicationAndPublishEvent() throws Exception {
        when(applicationEligibilityService.getEligibleJob(41L))
                .thenReturn(new JobEligibility(41L, "employer-7", "Security Engineer"));

        mockMvc.perform(post("/api/applications")
                        .header("X-User-Id", "student-77")
                        .header("X-User-Role", "STUDENT")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ApplicationCreateRequest(41L, "resume://student-77.pdf")
                        )))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", org.hamcrest.Matchers.matchesRegex(".*/api/applications/\\d+$")))
                .andExpect(jsonPath("$.status").value("SUBMITTED"))
                .andExpect(jsonPath("$.jobTitle").value("Security Engineer"));

        Application saved = applicationRepository.findAll().getFirst();
        org.junit.jupiter.api.Assertions.assertEquals("student-77", saved.getStudentId());
        org.junit.jupiter.api.Assertions.assertEquals(41L, saved.getJobId());
        org.junit.jupiter.api.Assertions.assertEquals(ApplicationStatus.SUBMITTED, saved.getStatus());
        verify(applicationEventPublisher).publishSubmitted(any());
    }

    @Test
    void withdrawApplication_ShouldUpdateStatusAndPublishEvent() throws Exception {
        when(applicationEligibilityService.getEligibleJob(52L))
                .thenReturn(new JobEligibility(52L, "employer-8", "QA Engineer"));

        mockMvc.perform(post("/api/applications")
                        .header("X-User-Id", "student-88")
                        .header("X-User-Role", "STUDENT")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ApplicationCreateRequest(52L, "resume://student-88.pdf")
                        )))
                .andExpect(status().isCreated());

        Long applicationId = applicationRepository.findAll().getFirst().getId();

        mockMvc.perform(put("/api/applications/{id}/withdraw", applicationId)
                        .header("X-User-Id", "student-88")
                        .header("X-User-Role", "STUDENT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("WITHDRAWN"))
                .andExpect(jsonPath("$.timeline[1].newStatus").value("WITHDRAWN"));

        Application saved = applicationRepository.findById(applicationId).orElseThrow();
        org.junit.jupiter.api.Assertions.assertEquals(ApplicationStatus.WITHDRAWN, saved.getStatus());
        verify(applicationEventPublisher).publishWithdrawn(any());
    }

    @Test
    void updateApplicationStatus_ShouldPersistTimelineAndPublishEvent() throws Exception {
        when(applicationEligibilityService.getEligibleJob(63L))
                .thenReturn(new JobEligibility(63L, "employer-9", "Platform Engineer"));
        when(applicationEligibilityService.getEmployerOwnedJob(63L, "employer-9"))
                .thenReturn(new JobDetailsResponse(63L, "employer-9", "Platform Engineer", "ACTIVE"));

        mockMvc.perform(post("/api/applications")
                        .header("X-User-Id", "student-99")
                        .header("X-User-Role", "STUDENT")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ApplicationCreateRequest(63L, "resume://student-99.pdf")
                        )))
                .andExpect(status().isCreated());

        Long applicationId = applicationRepository.findAll().getFirst().getId();

        mockMvc.perform(put("/api/applications/{id}/status", applicationId)
                        .header("X-User-Id", "employer-9")
                        .header("X-User-Role", "HIRING")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ApplicationStatusUpdateRequest(
                                        ApplicationStatus.UNDER_REVIEW,
                                        "Initial screening started"
                                )
                        )))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UNDER_REVIEW"))
                .andExpect(jsonPath("$.timeline[1].reason").value("Initial screening started"));

        mockMvc.perform(get("/api/applications/jobs/63")
                        .header("X-User-Id", "employer-9")
                        .header("X-User-Role", "HIRING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].studentId").value("student-99"));

        Application saved = applicationRepository.findById(applicationId).orElseThrow();
        org.junit.jupiter.api.Assertions.assertEquals(ApplicationStatus.UNDER_REVIEW, saved.getStatus());
        verify(applicationEventPublisher).publishStatusUpdated(any());
        verify(applicationEligibilityService, times(2)).getEmployerOwnedJob(eq(63L), eq("employer-9"));
    }
}
