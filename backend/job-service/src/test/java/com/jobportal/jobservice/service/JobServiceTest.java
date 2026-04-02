package com.jobportal.jobservice.service;

import com.jobportal.jobservice.dto.JobRequest;
import com.jobportal.jobservice.dto.JobResponse;
import com.jobportal.jobservice.dto.JobSearchCriteria;
import com.jobportal.jobservice.entity.Job;
import com.jobportal.jobservice.entity.Job.EmploymentType;
import com.jobportal.jobservice.entity.Job.JobStatus;
import com.jobportal.jobservice.exception.JobNotFoundException;
import com.jobportal.jobservice.repository.JobRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JobServiceTest {

    @Mock
    private JobRepository jobRepository;

    @InjectMocks
    private JobServiceImpl jobService;

    private Job testJob;
    private JobRequest testRequest;

    @BeforeEach
    void setUp() {
        testJob = new Job();
        testJob.setId(1L);
        testJob.setTitle("Software Engineer");
        testJob.setDescription("Build amazing software");
        testJob.setCompany("Tech Corp");
        testJob.setLocation("San Francisco, CA");
        testJob.setEmploymentType(EmploymentType.FULL_TIME);
        testJob.setSalaryMin(new BigDecimal("80000"));
        testJob.setSalaryMax(new BigDecimal("120000"));
        testJob.setSalaryCurrency("USD");
        testJob.setRequirements(Arrays.asList("Java", "Spring Boot"));
        testJob.setStatus(JobStatus.ACTIVE);
        testJob.setCreatedAt(LocalDateTime.now());
        testJob.setUpdatedAt(LocalDateTime.now());

        testRequest = new JobRequest();
        testRequest.setTitle("Software Engineer");
        testRequest.setDescription("Build amazing software");
        testRequest.setCompany("Tech Corp");
        testRequest.setLocation("San Francisco, CA");
        testRequest.setEmploymentType(EmploymentType.FULL_TIME);
        testRequest.setSalaryMin(new BigDecimal("80000"));
        testRequest.setSalaryMax(new BigDecimal("120000"));
        testRequest.setSalaryCurrency("USD");
        testRequest.setRequirements(Arrays.asList("Java", "Spring Boot"));
        testRequest.setStatus(JobStatus.ACTIVE);
    }

    @Test
    void createJob_ShouldReturnCreatedJob() {
        when(jobRepository.save(any(Job.class))).thenReturn(testJob);

        JobResponse response = jobService.createJob(testRequest, "employer-123");

        assertNotNull(response);
        assertEquals(testJob.getId(), response.getId());
        assertEquals(testJob.getTitle(), response.getTitle());
        assertEquals(testJob.getCompany(), response.getCompany());
        verify(jobRepository, times(1)).save(any(Job.class));
    }

    @Test
    void getJobById_WhenJobExists_ShouldReturnJob() {
        when(jobRepository.findById(1L)).thenReturn(Optional.of(testJob));

        JobResponse response = jobService.getJobById(1L);

        assertNotNull(response);
        assertEquals(testJob.getId(), response.getId());
        assertEquals(testJob.getTitle(), response.getTitle());
    }

    @Test
    void getJobById_WhenJobNotExists_ShouldThrowException() {
        when(jobRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(JobNotFoundException.class, () -> jobService.getJobById(99L));
    }

    @Test
    void getAllJobs_ShouldReturnPagedJobs() {
        Pageable pageable = PageRequest.of(0, 10);
        List<Job> jobs = Arrays.asList(testJob);
        Page<Job> jobPage = new PageImpl<>(jobs, pageable, jobs.size());

        when(jobRepository.findAll(pageable)).thenReturn(jobPage);

        Page<JobResponse> response = jobService.getAllJobs(pageable);

        assertNotNull(response);
        assertEquals(1, response.getTotalElements());
        assertEquals(testJob.getTitle(), response.getContent().get(0).getTitle());
    }

    @Test
    void updateJob_WhenJobExists_ShouldReturnUpdatedJob() {
        when(jobRepository.findById(1L)).thenReturn(Optional.of(testJob));
        when(jobRepository.save(any(Job.class))).thenReturn(testJob);

        testRequest.setTitle("Senior Software Engineer");
        JobResponse response = jobService.updateJob(1L, testRequest);

        assertNotNull(response);
        verify(jobRepository, times(1)).save(any(Job.class));
    }

    @Test
    void updateJob_WhenJobNotExists_ShouldThrowException() {
        when(jobRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(JobNotFoundException.class, () -> jobService.updateJob(99L, testRequest));
    }

    @Test
    void deleteJob_WhenJobExists_ShouldDeleteJob() {
        when(jobRepository.existsById(1L)).thenReturn(true);
        doNothing().when(jobRepository).deleteById(1L);

        jobService.deleteJob(1L);

        verify(jobRepository, times(1)).deleteById(1L);
    }

    @Test
    void deleteJob_WhenJobNotExists_ShouldThrowException() {
        when(jobRepository.existsById(99L)).thenReturn(false);

        assertThrows(JobNotFoundException.class, () -> jobService.deleteJob(99L));
    }

    @Test
    void searchJobs_ShouldReturnFilteredJobs() {
        Pageable pageable = PageRequest.of(0, 10);
        List<Job> jobs = Arrays.asList(testJob);
        Page<Job> jobPage = new PageImpl<>(jobs, pageable, jobs.size());

        JobSearchCriteria criteria = new JobSearchCriteria();
        criteria.setTitle("Software");
        criteria.setLocation("San Francisco");

        when(jobRepository.searchJobs(
                any(), any(), any(), any(), any(), any(), any(), any(), any()
        )).thenReturn(jobPage);

        Page<JobResponse> response = jobService.searchJobs(criteria, pageable);

        assertNotNull(response);
        assertEquals(1, response.getTotalElements());
    }
}
