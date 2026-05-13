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
import org.springframework.data.domain.Sort;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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

        testRequest = new JobRequest(
                "Software Engineer", "Build amazing software", "Tech Corp",
                "San Francisco, CA", EmploymentType.FULL_TIME,
                new BigDecimal("80000"), new BigDecimal("120000"), "USD",
                Arrays.asList("Java", "Spring Boot"), JobStatus.ACTIVE);
    }

    @Test
    void createJob_ShouldReturnCreatedJob() {
        when(jobRepository.save(any(Job.class))).thenReturn(testJob);

        JobResponse response = jobService.createJob(testRequest, "employer-123");

        assertNotNull(response);
        assertEquals(testJob.getId(), response.id());
        assertEquals(testJob.getTitle(), response.title());
        assertEquals(testJob.getCompany(), response.company());
        verify(jobRepository, times(1)).save(any(Job.class));
    }

    @Test
    void getJobById_WhenJobExists_ShouldReturnJob() {
        when(jobRepository.findById(1L)).thenReturn(Optional.of(testJob));

        JobResponse response = jobService.getJobById(1L);

        assertNotNull(response);
        assertEquals(testJob.getId(), response.id());
        assertEquals(testJob.getTitle(), response.title());
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
        assertEquals(testJob.getTitle(), response.getContent().get(0).title());
    }

    @Test
    void updateJob_WhenJobExists_ShouldReturnUpdatedJob() {
        when(jobRepository.findById(1L)).thenReturn(Optional.of(testJob));
        when(jobRepository.save(any(Job.class))).thenReturn(testJob);

        JobRequest updateRequest = new JobRequest(
                "Senior Software Engineer", testRequest.description(), testRequest.company(),
                testRequest.location(), testRequest.employmentType(),
                testRequest.salaryMin(), testRequest.salaryMax(), testRequest.salaryCurrency(),
                testRequest.requirements(), testRequest.status());
        JobResponse response = jobService.updateJob(1L, updateRequest);

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

        JobSearchCriteria criteria = new JobSearchCriteria("Software", null, "San Francisco", null, null, null, null, null);

        when(jobRepository.searchJobs(
                any(), any(), any(), any(), any(), any(), any(), any(), any()
        )).thenReturn(jobPage);

        Page<JobResponse> response = jobService.searchJobs(criteria, pageable);

        assertNotNull(response);
        assertEquals(1, response.getTotalElements());
    }

    @Test
    void searchJobs_ShouldForwardAllSearchFilters() {
        Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.ASC, "title"));
        Page<Job> jobPage = new PageImpl<>(List.of(testJob), pageable, 1);
        JobSearchCriteria criteria = new JobSearchCriteria(
                "Software",
                "Tech",
                "San Francisco",
                EmploymentType.FULL_TIME,
                new BigDecimal("70000"),
                new BigDecimal("130000"),
                JobStatus.ACTIVE,
                "employer-123"
        );

        when(jobRepository.searchJobs(
                eq("Software"),
                eq("Tech"),
                eq("San Francisco"),
                eq(EmploymentType.FULL_TIME),
                eq(new BigDecimal("70000")),
                eq(new BigDecimal("130000")),
                eq(JobStatus.ACTIVE),
                eq("employer-123"),
                eq(pageable)
        )).thenReturn(jobPage);

        Page<JobResponse> response = jobService.searchJobs(criteria, pageable);

        assertEquals(1, response.getTotalElements());
        verify(jobRepository).searchJobs(
                eq("Software"),
                eq("Tech"),
                eq("San Francisco"),
                eq(EmploymentType.FULL_TIME),
                eq(new BigDecimal("70000")),
                eq(new BigDecimal("130000")),
                eq(JobStatus.ACTIVE),
                eq("employer-123"),
                eq(pageable)
        );
    }

    @Test
    void searchJobs_WhenPageableIsUnsorted_ShouldUseCreatedAtDescendingSort() {
        Pageable unsortedPageable = PageRequest.of(0, 10);
        Page<Job> jobPage = new PageImpl<>(List.of(testJob), unsortedPageable, 1);
        JobSearchCriteria criteria = new JobSearchCriteria(null, null, null, null, null, null, null, null);

        when(jobRepository.searchJobs(
                any(), any(), any(), any(), any(), any(), any(), any(), any(Pageable.class)
        )).thenReturn(jobPage);

        jobService.searchJobs(criteria, unsortedPageable);

        verify(jobRepository).searchJobs(
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                argThat(pageable -> pageable.getSort().getOrderFor("createdAt") != null
                        && pageable.getSort().getOrderFor("createdAt").getDirection() == Sort.Direction.DESC)
        );
    }

    @Test
    void searchJobs_WhenPageableAlreadyHasSort_ShouldPreserveRequestedSort() {
        Pageable titleSortedPageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.ASC, "title"));
        Page<Job> jobPage = new PageImpl<>(List.of(testJob), titleSortedPageable, 1);
        JobSearchCriteria criteria = new JobSearchCriteria(null, null, null, null, null, null, null, null);

        when(jobRepository.searchJobs(
                any(), any(), any(), any(), any(), any(), any(), any(), any(Pageable.class)
        )).thenReturn(jobPage);

        jobService.searchJobs(criteria, titleSortedPageable);

        verify(jobRepository).searchJobs(
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                eq(titleSortedPageable)
        );
    }
}
