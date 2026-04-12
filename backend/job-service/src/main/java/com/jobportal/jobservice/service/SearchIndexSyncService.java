package com.jobportal.jobservice.service;

import com.jobportal.jobservice.dto.JobResponse;

public interface SearchIndexSyncService {

    void upsertJob(JobResponse job);

    void deleteJob(Long jobId);
}
