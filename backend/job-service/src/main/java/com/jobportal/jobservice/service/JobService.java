package com.jobportal.jobservice.service;

import com.jobportal.jobservice.dto.JobRequest;
import com.jobportal.jobservice.dto.JobResponse;
import com.jobportal.jobservice.dto.JobSearchCriteria;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface JobService {

    JobResponse createJob(JobRequest request);

    JobResponse getJobById(Long id);

    Page<JobResponse> getAllJobs(Pageable pageable);

    JobResponse updateJob(Long id, JobRequest request);

    void deleteJob(Long id);

    Page<JobResponse> searchJobs(JobSearchCriteria criteria, Pageable pageable);
}
