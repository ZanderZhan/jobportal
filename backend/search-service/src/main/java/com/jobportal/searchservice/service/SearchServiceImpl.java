package com.jobportal.searchservice.service;

import com.jobportal.searchservice.dto.JobSearchResult;
import com.jobportal.searchservice.dto.PagedResponse;
import com.jobportal.searchservice.exception.SearchServiceException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;
import java.net.URI;
import java.util.Optional;

@Service
public class SearchServiceImpl implements SearchService {

    private final RestTemplate restTemplate;
    private final String jobServiceUrl;

    public SearchServiceImpl(
            RestTemplate restTemplate,
            @Value("${services.job-service.url:http://localhost:8081}") String jobServiceUrl) {
        this.restTemplate = restTemplate;
        this.jobServiceUrl = jobServiceUrl;
    }

    @Override
    public PagedResponse<JobSearchResult> searchJobs(
            String title,
            String company,
            String location,
            String employmentType,
            BigDecimal salaryMin,
            BigDecimal salaryMax,
            String status,
            int page,
            int size,
            String sort) {

        String effectiveStatus = (status == null || status.isBlank()) ? "ACTIVE" : status;

        URI uri = UriComponentsBuilder.fromUriString(jobServiceUrl)
            .path("/api/jobs/search")
            .queryParamIfPresent("title", optional(title))
            .queryParamIfPresent("company", optional(company))
            .queryParamIfPresent("location", optional(location))
            .queryParamIfPresent("employmentType", optional(employmentType))
            .queryParamIfPresent("salaryMin", optional(salaryMin))
            .queryParamIfPresent("salaryMax", optional(salaryMax))
            .queryParam("status", effectiveStatus)
            .queryParam("page", page)
            .queryParam("size", size)
            .queryParam("sort", sort)
            .build(true)
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
                    "Job search returned an empty response",
                    502
                );
            }

            return response.getBody();
        } catch (RestClientException ex) {
            throw new SearchServiceException(
                "SEARCH_UPSTREAM_FAILED",
                "Failed to fetch search results from job-service",
                502
            );
        }
    }

    private <T> Optional<T> optional(T value) {
        return Optional.ofNullable(value);
    }
}
