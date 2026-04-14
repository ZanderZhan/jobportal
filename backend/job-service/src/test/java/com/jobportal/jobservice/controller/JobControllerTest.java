package com.jobportal.jobservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.jobportal.jobservice.dto.JobRequest;
import com.jobportal.jobservice.dto.JobResponse;
import com.jobportal.jobservice.entity.Job.EmploymentType;
import com.jobportal.jobservice.entity.Job.JobStatus;
import com.jobportal.jobservice.exception.JobNotFoundException;
import com.jobportal.jobservice.service.JobService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(JobController.class)
class JobControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private ObjectMapper objectMapper;

    @MockitoBean
    private JobService jobService;

    private JobResponse testJobResponse;
    private JobRequest testJobRequest;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        testJobResponse = new JobResponse(
                /* id */ 1L,
                /* employerId */ null,
                /* title */ "Software Engineer",
                /* description */ "Build amazing software",
                /* company */ "Tech Corp",
                /* location */ "San Francisco, CA",
                /* employmentType */ EmploymentType.FULL_TIME,
                /* salaryMin */ new BigDecimal("80000"),
                /* salaryMax */ new BigDecimal("120000"),
                /* salaryCurrency */ "USD",
                /* requirements */ Arrays.asList("Java", "Spring Boot"),
                /* status */ JobStatus.ACTIVE,
                /* createdAt */ LocalDateTime.now(),
                /* updatedAt */ LocalDateTime.now());

        testJobRequest = new JobRequest(
                /* title */ "Software Engineer",
                /* description */ "Build amazing software",
                /* company */ "Tech Corp",
                /* location */ "San Francisco, CA",
                /* employmentType */ EmploymentType.FULL_TIME,
                /* salaryMin */ new BigDecimal("80000"),
                /* salaryMax */ new BigDecimal("120000"),
                /* salaryCurrency */ "USD",
                /* requirements */ Arrays.asList("Java", "Spring Boot"),
                /* status */ JobStatus.ACTIVE);
    }

    @Test
    void createJob_ValidRequest_ShouldReturnCreated() throws Exception {
        when(jobService.createJob(any(JobRequest.class), any())).thenReturn(testJobResponse);

        mockMvc.perform(post("/api/jobs")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testJobRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.title").value("Software Engineer"))
                .andExpect(jsonPath("$.company").value("Tech Corp"));
    }

    @Test
    void createJob_InvalidRequest_ShouldReturnBadRequest() throws Exception {
        JobRequest invalidRequest = new JobRequest(null, null, null, null, null, null, null, null, null, null);

        mockMvc.perform(post("/api/jobs")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
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
    void updateJob_WhenExists_ShouldReturnUpdatedJob() throws Exception {
        when(jobService.getJobById(1L)).thenReturn(testJobResponse);
        when(jobService.updateJob(eq(1L), any(JobRequest.class))).thenReturn(testJobResponse);

        mockMvc.perform(put("/api/jobs/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testJobRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void updateJob_WhenNotExists_ShouldReturnNotFound() throws Exception {
        when(jobService.getJobById(99L)).thenThrow(new JobNotFoundException(99L));

        mockMvc.perform(put("/api/jobs/99")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testJobRequest)))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteJob_WhenExists_ShouldReturnNoContent() throws Exception {
        when(jobService.getJobById(1L)).thenReturn(testJobResponse);
        doNothing().when(jobService).deleteJob(1L);

        mockMvc.perform(delete("/api/jobs/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteJob_WhenNotExists_ShouldReturnNotFound() throws Exception {
        when(jobService.getJobById(99L)).thenThrow(new JobNotFoundException(99L));

        mockMvc.perform(delete("/api/jobs/99"))
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
}
