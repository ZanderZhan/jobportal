package com.jobportal.jobservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.jobportal.jobservice.dto.JobRequest;
import com.jobportal.jobservice.dto.JobResponse;
import com.jobportal.jobservice.entity.Job.EmploymentType;
import com.jobportal.jobservice.entity.Job.JobStatus;
import com.jobportal.jobservice.exception.JobNotFoundException;
import com.jobportal.jobservice.config.SecurityConfig;
import com.jobportal.jobservice.service.JobService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(JobController.class)
@Import(SecurityConfig.class)
class JobControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private ObjectMapper objectMapper;

    @MockitoBean
    private JobService jobService;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    private JobResponse testJobResponse;
    private JobRequest testJobRequest;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        testJobResponse = new JobResponse(
                1L,
                "employer-123",
                "Software Engineer",
                "Build amazing software",
                "Tech Corp",
                "San Francisco, CA",
                EmploymentType.FULL_TIME,
                new BigDecimal("80000"),
                new BigDecimal("120000"),
                "USD",
                Arrays.asList("Java", "Spring Boot"),
                JobStatus.ACTIVE,
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        testJobRequest = new JobRequest(
                "Software Engineer",
                "Build amazing software",
                "Tech Corp",
                "San Francisco, CA",
                EmploymentType.FULL_TIME,
                new BigDecimal("80000"),
                new BigDecimal("120000"),
                "USD",
                Arrays.asList("Java", "Spring Boot"),
                JobStatus.ACTIVE
        );
    }

    @Test
    void createJob_WithHiringRole_ShouldReturnCreated() throws Exception {
        when(jobService.createJob(any(JobRequest.class), eq("employer-123"))).thenReturn(testJobResponse);

        mockMvc.perform(post("/api/jobs")
                        .with(hiringJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testJobRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.title").value("Software Engineer"));
    }

    @Test
    void createJob_WithoutToken_ShouldReturnUnauthorized() throws Exception {
        mockMvc.perform(post("/api/jobs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testJobRequest)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createJob_WithStudentRole_ShouldReturnForbidden() throws Exception {
        mockMvc.perform(post("/api/jobs")
                        .with(studentJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testJobRequest)))
                .andExpect(status().isForbidden());
    }

    @Test
    void getJobById_WhenExists_ShouldReturnJob() throws Exception {
        when(jobService.getJobById(1L)).thenReturn(testJobResponse);

        mockMvc.perform(get("/api/jobs/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.title").value("Software Engineer"));
    }

    @Test
    void getJobById_WhenNotExists_ShouldReturnNotFound() throws Exception {
        when(jobService.getJobById(99L)).thenThrow(new JobNotFoundException(99L));

        mockMvc.perform(get("/api/jobs/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getAllJobs_ShouldReturnPagedJobs() throws Exception {
        List<JobResponse> jobs = Arrays.asList(testJobResponse);
        Page<JobResponse> page = new PageImpl<>(jobs);

        when(jobService.getAllJobs(any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/jobs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].id").value(1));
    }

    @Test
    void updateJob_WhenOwnedAndHiringRole_ShouldReturnUpdatedJob() throws Exception {
        when(jobService.updateJob(eq(1L), any(JobRequest.class), eq("employer-123"))).thenReturn(testJobResponse);

        mockMvc.perform(put("/api/jobs/1")
                        .with(hiringJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testJobRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void updateJob_WhenWithoutToken_ShouldReturnUnauthorized() throws Exception {
        mockMvc.perform(put("/api/jobs/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testJobRequest)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void updateJob_WhenNotExists_ShouldReturnNotFound() throws Exception {
        when(jobService.updateJob(eq(99L), any(JobRequest.class), eq("employer-123")))
                .thenThrow(new JobNotFoundException(99L));

        mockMvc.perform(put("/api/jobs/99")
                        .with(hiringJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testJobRequest)))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteJob_WhenOwnedAndHiringRole_ShouldReturnNoContent() throws Exception {
        mockMvc.perform(delete("/api/jobs/1").with(hiringJwt()))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteJob_WhenNotExists_ShouldReturnNotFound() throws Exception {
        org.mockito.Mockito.doThrow(new JobNotFoundException(99L))
                .when(jobService).deleteJob(99L, "employer-123");

        mockMvc.perform(delete("/api/jobs/99").with(hiringJwt()))
                .andExpect(status().isNotFound());
    }

    @Test
    void searchJobs_WithFilters_ShouldReturnFilteredResults() throws Exception {
        List<JobResponse> jobs = Arrays.asList(testJobResponse);
        Page<JobResponse> page = new PageImpl<>(jobs);

        when(jobService.searchJobs(any(), any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/jobs/search")
                        .param("title", "Software")
                        .param("location", "San Francisco"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].title").value("Software Engineer"));
    }

    private JwtRequestPostProcessor hiringJwt() {
        return jwt()
                .jwt(builder -> builder
                        .subject("employer-123")
                        .claim("role", "HIRING"))
                .authorities(new SimpleGrantedAuthority("ROLE_HIRING"));
    }

    private JwtRequestPostProcessor studentJwt() {
        return jwt()
                .jwt(builder -> builder
                        .subject("student-123")
                        .claim("role", "STUDENT"))
                .authorities(new SimpleGrantedAuthority("ROLE_STUDENT"));
    }
}
