package com.jobportal.searchservice.repository;

import com.jobportal.searchservice.dto.JobSearchFacetsResponse;
import com.jobportal.searchservice.dto.JobSearchResult;
import com.jobportal.searchservice.dto.PagedResponse;
import com.jobportal.searchservice.dto.SearchIndexStatusResponse;
import com.jobportal.searchservice.service.SearchRequest;

import java.util.List;

public interface SearchIndexRepository {

    SearchIndexStatusResponse getStatus();

    PagedResponse<JobSearchResult> search(SearchRequest request);

    JobSearchFacetsResponse getFacets(SearchRequest request, int maxValues);

    void upsert(JobSearchResult job);

    void upsertAll(List<JobSearchResult> jobs);

    void deleteById(Long id);

    void deleteAll();

    void markReindexStarted();

    void markReindexCompleted(long documentCount);

    void markReindexFailed();

    void markIncrementalSync();

    long countDocuments();
}
