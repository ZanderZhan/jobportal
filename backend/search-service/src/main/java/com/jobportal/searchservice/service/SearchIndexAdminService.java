package com.jobportal.searchservice.service;

import com.jobportal.searchservice.dto.JobSearchResult;
import com.jobportal.searchservice.dto.SearchIndexStatusResponse;

public interface SearchIndexAdminService {

    SearchIndexStatusResponse getStatus();

    SearchIndexStatusResponse reindexJobs();

    void upsertJob(Long id, JobSearchResult job);

    void deleteJob(Long id);
}
