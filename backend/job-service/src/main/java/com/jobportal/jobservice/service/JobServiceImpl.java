package com.jobportal.jobservice.service;

import com.jobportal.jobservice.dto.JobRequest;
import com.jobportal.jobservice.dto.JobResponse;
import com.jobportal.jobservice.dto.JobSearchCriteria;
import com.jobportal.jobservice.entity.Job;
import com.jobportal.jobservice.exception.JobNotFoundException;
import com.jobportal.jobservice.repository.JobRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@Transactional
public class JobServiceImpl implements JobService {

    private final JobRepository jobRepository;

    public JobServiceImpl(JobRepository jobRepository) {
        this.jobRepository = jobRepository;
    }

    @Override
    public JobResponse createJob(JobRequest request, String employerId) {
        String callerId = requireAuthenticatedCallerId(employerId);
        Job job = mapRequestToEntity(request, new Job());
        job.setEmployerId(callerId);
        Job savedJob = jobRepository.save(job);
        return JobResponse.fromEntity(savedJob);
    }

    @Override
    @Transactional(readOnly = true)
    public JobResponse getJobById(Long id) {
        Job job = jobRepository.findById(id)
                .orElseThrow(() -> new JobNotFoundException(id));
        return JobResponse.fromEntity(job);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<JobResponse> getAllJobs(Pageable pageable) {
        return jobRepository.findAll(pageable)
                .map(JobResponse::fromEntity);
    }

    @Override
    public JobResponse updateJob(Long id, JobRequest request, String callerId) {
        Job existingJob = jobRepository.findById(id)
                .orElseThrow(() -> new JobNotFoundException(id));
        ensureOwnership(existingJob, callerId);

        Job updatedJob = mapRequestToEntity(request, existingJob);
        Job savedJob = jobRepository.save(updatedJob);
        return JobResponse.fromEntity(savedJob);
    }

    @Override
    public void deleteJob(Long id, String callerId) {
        Job existingJob = jobRepository.findById(id)
                .orElseThrow(() -> new JobNotFoundException(id));
        ensureOwnership(existingJob, callerId);
        jobRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<JobResponse> searchJobs(JobSearchCriteria criteria, Pageable pageable) {
        return jobRepository.searchJobs(
                criteria.title(),
                criteria.company(),
                criteria.location(),
                criteria.employmentType(),
                criteria.salaryMin(),
                criteria.salaryMax(),
                criteria.status(),
                criteria.employerId(),
                pageable
        ).map(JobResponse::fromEntity);
    }

    private Job mapRequestToEntity(JobRequest request, Job job) {
        job.setTitle(request.title());
        job.setDescription(request.description());
        job.setCompany(request.company());
        job.setLocation(request.location());
        job.setEmploymentType(request.employmentType());
        job.setSalaryMin(request.salaryMin());
        job.setSalaryMax(request.salaryMax());
        job.setSalaryCurrency(request.salaryCurrency());
        job.setRequirements(request.requirements());
        if (request.status() != null) {
            job.setStatus(request.status());
        }
        return job;
    }

    private String requireAuthenticatedCallerId(String callerId) {
        if (!StringUtils.hasText(callerId)) {
            throw new AccessDeniedException("Authenticated employer identity is required.");
        }
        return callerId;
    }

    private void ensureOwnership(Job existingJob, String callerId) {
        String resolvedCallerId = requireAuthenticatedCallerId(callerId);
        String ownerId = existingJob.getEmployerId();
        if (!StringUtils.hasText(ownerId) || !ownerId.equals(resolvedCallerId)) {
            throw new AccessDeniedException("Forbidden – caller does not own this job.");
        }
    }

}
