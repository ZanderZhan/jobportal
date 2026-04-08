package com.jobportal.jobservice.service;

import com.jobportal.jobservice.dto.JobResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

@Service
public class SearchIndexSyncServiceImpl implements SearchIndexSyncService {

    private static final Logger logger = LoggerFactory.getLogger(SearchIndexSyncServiceImpl.class);

    private final RestTemplate restTemplate;
    private final String searchServiceUrl;

    public SearchIndexSyncServiceImpl(
            RestTemplate restTemplate,
            @Value("${services.search-service.url:http://localhost:8083}") String searchServiceUrl) {
        this.restTemplate = restTemplate;
        this.searchServiceUrl = searchServiceUrl;
    }

    @Override
    public void upsertJob(JobResponse job) {
        URI uri = UriComponentsBuilder.fromUriString(searchServiceUrl)
            .path("/internal/search/index/jobs/{id}")
            .build(job.getId());

        try {
            restTemplate.exchange(
                uri,
                HttpMethod.PUT,
                new HttpEntity<>(job),
                Void.class
            );
        } catch (RestClientException ex) {
            logger.warn("Failed to sync job {} to search-service index", job.getId(), ex);
        }
    }

    @Override
    public void deleteJob(Long jobId) {
        URI uri = UriComponentsBuilder.fromUriString(searchServiceUrl)
            .path("/internal/search/index/jobs/{id}")
            .build(jobId);

        try {
            restTemplate.exchange(
                uri,
                HttpMethod.DELETE,
                HttpEntity.EMPTY,
                Void.class
            );
        } catch (RestClientException ex) {
            logger.warn("Failed to delete job {} from search-service index", jobId, ex);
        }
    }
}
