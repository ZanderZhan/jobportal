package com.jobportal.searchservice.service;

import com.jobportal.searchservice.dto.JobSearchResult;
import com.jobportal.searchservice.dto.PagedResponse;
import com.jobportal.searchservice.exception.SearchServiceException;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
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
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

@Service
public class SearchServiceImpl implements SearchService {

    private static final String DEFAULT_STATUS = "ACTIVE";
    private static final String DEFAULT_SORT = "createdAt,desc";
    private static final Set<String> VALID_EMPLOYMENT_TYPES = Set.of(
        "FULL_TIME",
        "PART_TIME",
        "CONTRACT",
        "INTERNSHIP"
    );
    private static final Set<String> VALID_STATUSES = Set.of(
        "DRAFT",
        "ACTIVE",
        "CLOSED"
    );
    private static final Set<String> VALID_SORT_FIELDS = Set.of(
        "title",
        "company",
        "location",
        "employmentType",
        "salaryMin",
        "salaryMax",
        "status",
        "createdAt",
        "updatedAt"
    );

    private final RestTemplate restTemplate;
    private final MeterRegistry meterRegistry;
    private final String jobServiceUrl;
    private final int maxPageSize;

    public SearchServiceImpl(
            RestTemplate restTemplate,
            MeterRegistry meterRegistry,
            @Value("${services.job-service.url:http://localhost:8081}") String jobServiceUrl,
            @Value("${search.pagination.max-size:100}") int maxPageSize) {
        this.restTemplate = restTemplate;
        this.meterRegistry = meterRegistry;
        this.jobServiceUrl = jobServiceUrl;
        this.maxPageSize = maxPageSize;
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

        Timer.Sample sample = Timer.start(meterRegistry);
        String outcome = "success";

        try {
            String normalizedTitle = normalizeText(title);
            String normalizedCompany = normalizeText(company);
            String normalizedLocation = normalizeText(location);
            String normalizedEmploymentType = normalizeEnumValue(employmentType);
            String normalizedStatus = normalizeEnumValue(status);
            BigDecimal normalizedSalaryMin = normalizeSalary(salaryMin, "salaryMin");
            BigDecimal normalizedSalaryMax = normalizeSalary(salaryMax, "salaryMax");
            int validatedPage = validatePage(page);
            int cappedSize = capPageSize(size);
            String validatedSort = normalizeSort(sort);

            validateEnumValue("employmentType", normalizedEmploymentType, VALID_EMPLOYMENT_TYPES);
            validateEnumValue("status", normalizedStatus, VALID_STATUSES);
            validateSalaryRange(normalizedSalaryMin, normalizedSalaryMax);

            String effectiveStatus = normalizedStatus == null ? DEFAULT_STATUS : normalizedStatus;

            URI uri = UriComponentsBuilder.fromUriString(jobServiceUrl)
                .path("/api/jobs/search")
                .queryParamIfPresent("title", optional(normalizedTitle))
                .queryParamIfPresent("company", optional(normalizedCompany))
                .queryParamIfPresent("location", optional(normalizedLocation))
                .queryParamIfPresent("employmentType", optional(normalizedEmploymentType))
                .queryParamIfPresent("salaryMin", optional(normalizedSalaryMin))
                .queryParamIfPresent("salaryMax", optional(normalizedSalaryMax))
                .queryParam("status", effectiveStatus)
                .queryParam("page", validatedPage)
                .queryParam("size", cappedSize)
                .queryParam("sort", validatedSort)
                .build()
                .encode()
                .toUri();

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

            incrementRequestCounter("success");
            return response.getBody();
        } catch (SearchServiceException ex) {
            outcome = metricOutcome(ex);
            incrementRequestCounter(outcome);
            if ("upstream_error".equals(outcome)) {
                incrementUpstreamErrorCounter(ex.getErrorCode());
            }
            throw ex;
        } catch (RestClientException ex) {
            outcome = "upstream_error";
            incrementRequestCounter(outcome);
            incrementUpstreamErrorCounter("SEARCH_UPSTREAM_FAILED");
            throw new SearchServiceException(
                "SEARCH_UPSTREAM_FAILED",
                "Failed to fetch search results from job-service",
                502
            );
        } finally {
            sample.stop(Timer.builder("search.latency")
                .description("Latency for search requests")
                .tag("outcome", outcome)
                .register(meterRegistry));
        }
    }

    private <T> Optional<T> optional(T value) {
        return Optional.ofNullable(value);
    }

    private String normalizeText(String value) {
        if (value == null) {
            return null;
        }

        String normalized = value.trim().replaceAll("\\s+", " ");
        return normalized.isEmpty() ? null : normalized;
    }

    private String normalizeEnumValue(String value) {
        String normalized = normalizeText(value);
        if (normalized == null) {
            return null;
        }

        return normalized
            .replace('-', '_')
            .replace(' ', '_')
            .toUpperCase(Locale.ROOT);
    }

    private BigDecimal normalizeSalary(BigDecimal value, String fieldName) {
        if (value == null) {
            return null;
        }

        if (value.signum() < 0) {
            throw invalidRequest(fieldName + " must be greater than or equal to 0");
        }

        return value;
    }

    private int validatePage(int page) {
        if (page < 0) {
            throw invalidRequest("page must be greater than or equal to 0");
        }

        return page;
    }

    private int capPageSize(int size) {
        if (size < 1) {
            throw invalidRequest("size must be greater than or equal to 1");
        }

        return Math.min(size, maxPageSize);
    }

    private void validateEnumValue(String fieldName, String value, Set<String> allowedValues) {
        if (value != null && !allowedValues.contains(value)) {
            throw invalidRequest(fieldName + " must be one of: " + String.join(", ", allowedValues));
        }
    }

    private void validateSalaryRange(BigDecimal salaryMin, BigDecimal salaryMax) {
        if (salaryMin != null && salaryMax != null && salaryMin.compareTo(salaryMax) > 0) {
            throw invalidRequest("salaryMin must be less than or equal to salaryMax");
        }
    }

    private String normalizeSort(String sort) {
        String normalized = normalizeText(sort);
        if (normalized == null) {
            return DEFAULT_SORT;
        }

        String[] parts = normalized.split(",");
        if (parts.length != 2) {
            throw invalidRequest("sort must follow the format field,direction");
        }

        String field = canonicalSortField(parts[0].trim());
        String direction = parts[1].trim().toLowerCase(Locale.ROOT);

        if (!"asc".equals(direction) && !"desc".equals(direction)) {
            throw invalidRequest("sort direction must be asc or desc");
        }

        return field + "," + direction;
    }

    private String canonicalSortField(String field) {
        if (field.isBlank()) {
            throw invalidRequest("sort field is required");
        }

        return VALID_SORT_FIELDS.stream()
            .filter(candidate -> candidate.equalsIgnoreCase(field))
            .findFirst()
            .orElseThrow(() -> invalidRequest("sort field must be one of: " + String.join(", ", VALID_SORT_FIELDS)));
    }

    private SearchServiceException invalidRequest(String message) {
        return new SearchServiceException(
            "SEARCH_INVALID_REQUEST",
            message,
            400
        );
    }

    private String metricOutcome(SearchServiceException ex) {
        return ex.getHttpStatus() >= 500 ? "upstream_error" : "validation_error";
    }

    private void incrementRequestCounter(String outcome) {
        meterRegistry.counter("search.requests", "outcome", outcome).increment();
    }

    private void incrementUpstreamErrorCounter(String code) {
        meterRegistry.counter("search.upstream.errors", "code", code).increment();
    }
}
