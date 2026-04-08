package com.jobportal.jobservice.service;

import com.jobportal.jobservice.dto.JobRequest;
import com.jobportal.jobservice.dto.JobResponse;
import com.jobportal.jobservice.dto.JobSearchCriteria;
import com.jobportal.jobservice.entity.Job;
import com.jobportal.jobservice.exception.JobNotFoundException;
import com.jobportal.jobservice.repository.JobRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class JobServiceImpl implements JobService {

    private final JobRepository jobRepository;
    private final SearchIndexSyncService searchIndexSyncService;

    public JobServiceImpl(JobRepository jobRepository, SearchIndexSyncService searchIndexSyncService) {
        this.jobRepository = jobRepository;
        this.searchIndexSyncService = searchIndexSyncService;
    }

    @Override
    public JobResponse createJob(JobRequest request) {
        Job job = mapRequestToEntity(request, new Job());
        Job savedJob = jobRepository.save(job);
        JobResponse response = JobResponse.fromEntity(savedJob);
        runAfterCommitOrImmediately(() -> searchIndexSyncService.upsertJob(response));
        return response;
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
    public JobResponse updateJob(Long id, JobRequest request) {
        Job existingJob = jobRepository.findById(id)
                .orElseThrow(() -> new JobNotFoundException(id));

        Job updatedJob = mapRequestToEntity(request, existingJob);
        Job savedJob = jobRepository.save(updatedJob);
        JobResponse response = JobResponse.fromEntity(savedJob);
        runAfterCommitOrImmediately(() -> searchIndexSyncService.upsertJob(response));
        return response;
    }

    @Override
    public void deleteJob(Long id) {
        if (!jobRepository.existsById(id)) {
            throw new JobNotFoundException(id);
        }
        jobRepository.deleteById(id);
        runAfterCommitOrImmediately(() -> searchIndexSyncService.deleteJob(id));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<JobResponse> searchJobs(JobSearchCriteria criteria, Pageable pageable) {
        return jobRepository.searchJobs(
                criteria.getTitle(),
                criteria.getCompany(),
                criteria.getLocation(),
                criteria.getEmploymentType(),
                criteria.getSalaryMin(),
                criteria.getSalaryMax(),
                criteria.getStatus(),
                pageable
        ).map(JobResponse::fromEntity);
    }

    private Job mapRequestToEntity(JobRequest request, Job job) {
        job.setTitle(request.getTitle());
        job.setDescription(request.getDescription());
        job.setCompany(request.getCompany());
        job.setLocation(request.getLocation());
        job.setEmploymentType(request.getEmploymentType());
        job.setSalaryMin(request.getSalaryMin());
        job.setSalaryMax(request.getSalaryMax());
        job.setSalaryCurrency(request.getSalaryCurrency());
        job.setRequirements(request.getRequirements());
        if (request.getStatus() != null) {
            job.setStatus(request.getStatus());
        }
        return job;
    }

    private void runAfterCommitOrImmediately(Runnable task) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    task.run();
                }
            });
            return;
        }

        task.run();
    }
}
