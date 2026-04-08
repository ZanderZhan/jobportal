package com.jobportal.searchservice.service;

import com.jobportal.searchservice.dto.JobSearchResult;
import com.jobportal.searchservice.dto.PagedResponse;
import com.jobportal.searchservice.dto.SearchIndexStatusResponse;
import com.jobportal.searchservice.exception.SearchServiceException;
import com.jobportal.searchservice.repository.SearchIndexRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

@Service
public class SearchIndexAdminServiceImpl implements SearchIndexAdminService {

    private final SearchIndexRepository searchIndexRepository;
    private final SearchCacheManager searchCacheManager;
    private final RestTemplate restTemplate;
    private final String jobServiceUrl;
    private final int reindexPageSize;

    public SearchIndexAdminServiceImpl(
            SearchIndexRepository searchIndexRepository,
            SearchCacheManager searchCacheManager,
            RestTemplate restTemplate,
            @Value("${services.job-service.url:http://localhost:8081}") String jobServiceUrl,
            @Value("${search.index.reindex-page-size:200}") int reindexPageSize) {
        this.searchIndexRepository = searchIndexRepository;
        this.searchCacheManager = searchCacheManager;
        this.restTemplate = restTemplate;
        this.jobServiceUrl = jobServiceUrl;
        this.reindexPageSize = reindexPageSize;
    }

    @Override
    public SearchIndexStatusResponse getStatus() {
        try {
            return searchIndexRepository.getStatus();
        } catch (DataAccessException ex) {
            throw new SearchServiceException(
                "SEARCH_INDEX_UNAVAILABLE",
                "Search index is unavailable",
                503
            );
        }
    }

    @Override
    public SearchIndexStatusResponse reindexJobs() {
        try {
            searchIndexRepository.markReindexStarted();
            searchIndexRepository.deleteAll();
            searchCacheManager.clearAll();

            int page = 0;
            long indexedCount = 0;
            boolean hasNext = true;

            while (hasNext) {
                PagedResponse<JobSearchResult> upstreamPage = fetchJobsPage(page);
                searchIndexRepository.upsertAll(upstreamPage.getContent());
                indexedCount += upstreamPage.getContent().size();
                hasNext = !upstreamPage.isLast();
                page++;
            }

            searchIndexRepository.markReindexCompleted(indexedCount);
            searchCacheManager.clearAll();
            return searchIndexRepository.getStatus();
        } catch (SearchServiceException ex) {
            searchIndexRepository.markReindexFailed();
            throw ex;
        } catch (DataAccessException ex) {
            searchIndexRepository.markReindexFailed();
            throw new SearchServiceException(
                "SEARCH_INDEX_REINDEX_FAILED",
                "Failed to rebuild the search index",
                503
            );
        }
    }

    @Override
    public void upsertJob(Long id, JobSearchResult job) {
        if (job == null) {
            throw new SearchServiceException(
                "SEARCH_INVALID_REQUEST",
                "Indexed job payload is required",
                400
            );
        }

        if (job.getId() == null) {
            job.setId(id);
        } else if (!id.equals(job.getId())) {
            throw new SearchServiceException(
                "SEARCH_INVALID_REQUEST",
                "Indexed job payload id must match the request path",
                400
            );
        }

        try {
            searchIndexRepository.upsert(job);
            searchIndexRepository.markIncrementalSync();
            searchCacheManager.clearAll();
        } catch (DataAccessException ex) {
            throw new SearchServiceException(
                "SEARCH_INDEX_UPDATE_FAILED",
                "Failed to update the search index",
                503
            );
        }
    }

    @Override
    public void deleteJob(Long id) {
        try {
            searchIndexRepository.deleteById(id);
            searchIndexRepository.markIncrementalSync();
            searchCacheManager.clearAll();
        } catch (DataAccessException ex) {
            throw new SearchServiceException(
                "SEARCH_INDEX_UPDATE_FAILED",
                "Failed to update the search index",
                503
            );
        }
    }

    private PagedResponse<JobSearchResult> fetchJobsPage(int page) {
        URI uri = UriComponentsBuilder.fromUriString(jobServiceUrl)
            .path("/api/jobs")
            .queryParam("page", page)
            .queryParam("size", reindexPageSize)
            .queryParam("sort", "updatedAt,desc")
            .build()
            .encode()
            .toUri();

        try {
            ResponseEntity<PagedResponse<JobSearchResult>> response = restTemplate.exchange(
                uri,
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<>() {}
            );

            if (response.getBody() == null) {
                throw new SearchServiceException(
                    "SEARCH_UPSTREAM_EMPTY",
                    "Job reindex returned an empty response",
                    502
                );
            }

            return response.getBody();
        } catch (RestClientException ex) {
            throw new SearchServiceException(
                "SEARCH_UPSTREAM_FAILED",
                "Failed to fetch jobs for reindexing from job-service",
                502
            );
        }
    }
}
