package com.jobportal.searchservice.service;

import com.jobportal.searchservice.dto.FacetValueCount;
import com.jobportal.searchservice.dto.JobSearchFacetsResponse;
import com.jobportal.searchservice.dto.JobSearchResult;
import com.jobportal.searchservice.dto.PagedResponse;
import com.jobportal.searchservice.dto.SearchIndexStatusResponse;
import com.jobportal.searchservice.exception.SearchServiceException;
import com.jobportal.searchservice.repository.SearchIndexRepository;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.beans.factory.annotation.Autowired;
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

import java.math.BigDecimal;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class SearchServiceImpl implements SearchService {

    private static final String DEFAULT_STATUS = "ACTIVE";
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

    private final SearchIndexRepository searchIndexRepository;
    private final SearchCacheManager searchCacheManager;
    private final RestTemplate restTemplate;
    private final MeterRegistry meterRegistry;
    private final String jobServiceUrl;
    private final int maxPageSize;
    private final int maxCandidateWindow;
    private final int facetSampleSize;
    private final int facetMaxValues;
    private final int retryMaxAttempts;
    private final long retryBackoffMs;
    private final int circuitFailureThreshold;
    private final long circuitOpenSeconds;
    private final AtomicInteger consecutiveUpstreamFailures = new AtomicInteger();
    private volatile Instant circuitOpenedAt;

    @Autowired
    public SearchServiceImpl(
            SearchIndexRepository searchIndexRepository,
            SearchCacheManager searchCacheManager,
            RestTemplate restTemplate,
            MeterRegistry meterRegistry,
            @Value("${services.job-service.url:http://localhost:8081}") String jobServiceUrl,
            @Value("${search.pagination.max-size:100}") int maxPageSize,
            @Value("${search.ranking.max-candidate-window:200}") int maxCandidateWindow,
            @Value("${search.facets.sample-size:200}") int facetSampleSize,
            @Value("${search.facets.max-values:10}") int facetMaxValues,
            @Value("${search.resilience.retry.max-attempts:2}") int retryMaxAttempts,
            @Value("${search.resilience.retry.backoff-ms:100}") long retryBackoffMs,
            @Value("${search.resilience.circuit-breaker.failure-threshold:3}") int circuitFailureThreshold,
            @Value("${search.resilience.circuit-breaker.open-seconds:30}") long circuitOpenSeconds) {
        this.searchIndexRepository = searchIndexRepository;
        this.searchCacheManager = searchCacheManager;
        this.restTemplate = restTemplate;
        this.meterRegistry = meterRegistry;
        this.jobServiceUrl = jobServiceUrl;
        this.maxPageSize = maxPageSize;
        this.maxCandidateWindow = maxCandidateWindow;
        this.facetSampleSize = facetSampleSize;
        this.facetMaxValues = facetMaxValues;
        this.retryMaxAttempts = retryMaxAttempts;
        this.retryBackoffMs = retryBackoffMs;
        this.circuitFailureThreshold = circuitFailureThreshold;
        this.circuitOpenSeconds = circuitOpenSeconds;
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
            SearchRequest request = normalizeRequest(
                title,
                company,
                location,
                employmentType,
                salaryMin,
                salaryMax,
                status,
                page,
                size,
                sort
            );

            PagedResponse<JobSearchResult> cachedResponse = searchCacheManager.getSearch(request.cacheKey());
            if (cachedResponse != null) {
                incrementRequestCounter("cache_hit");
                return cachedResponse;
            }

            PagedResponse<JobSearchResult> response = searchViaIndexOrFallback(request);
            searchCacheManager.putSearch(request.cacheKey(), response);
            incrementRequestCounter("success");
            return response;
        } catch (SearchServiceException ex) {
            outcome = metricOutcome(ex);
            incrementRequestCounter(outcome);
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

    @Override
    public JobSearchFacetsResponse getJobSearchFacets(
            String title,
            String company,
            String location,
            String employmentType,
            BigDecimal salaryMin,
            BigDecimal salaryMax,
            String status) {

        Timer.Sample sample = Timer.start(meterRegistry);
        String outcome = "success";

        try {
            SearchRequest request = normalizeRequest(
                title,
                company,
                location,
                employmentType,
                salaryMin,
                salaryMax,
                status,
                0,
                Math.min(facetSampleSize, maxCandidateWindow),
                SearchRequest.DEFAULT_SORT
            );

            JobSearchFacetsResponse cachedResponse = searchCacheManager.getFacets(request.facetsCacheKey());
            if (cachedResponse != null) {
                incrementRequestCounter("cache_hit");
                return cachedResponse;
            }

            JobSearchFacetsResponse response = facetsViaIndexOrFallback(request);
            searchCacheManager.putFacets(request.facetsCacheKey(), response);
            incrementRequestCounter("success");
            return response;
        } catch (SearchServiceException ex) {
            outcome = metricOutcome(ex);
            incrementRequestCounter(outcome);
            throw ex;
        } finally {
            sample.stop(Timer.builder("search.latency")
                .description("Latency for search requests")
                .tag("outcome", outcome)
                .register(meterRegistry));
        }
    }

    private PagedResponse<JobSearchResult> searchViaIndexOrFallback(SearchRequest request) {
        try {
            SearchIndexStatusResponse indexStatus = searchIndexRepository.getStatus();
            if (indexStatus.isReady() && !indexStatus.isReindexInProgress()) {
                return searchIndexRepository.search(request);
            }
            incrementIndexFallbackCounter(indexStatus.isReindexInProgress() ? "reindex_in_progress" : "index_not_ready");
        } catch (DataAccessException ex) {
            incrementIndexFallbackCounter("index_unavailable");
        }

        return request.usesDefaultSort() && request.offset() < maxCandidateWindow
            ? buildRankedResponse(request)
            : fetchUpstreamPage(request, request.page(), request.size(), request.sort());
    }

    private JobSearchFacetsResponse facetsViaIndexOrFallback(SearchRequest request) {
        try {
            SearchIndexStatusResponse indexStatus = searchIndexRepository.getStatus();
            if (indexStatus.isReady() && !indexStatus.isReindexInProgress()) {
                return searchIndexRepository.getFacets(request, facetMaxValues);
            }
            incrementIndexFallbackCounter(indexStatus.isReindexInProgress() ? "reindex_in_progress" : "index_not_ready");
        } catch (DataAccessException ex) {
            incrementIndexFallbackCounter("index_unavailable");
        }

        PagedResponse<JobSearchResult> upstreamResponse = fetchUpstreamPage(
            request,
            0,
            Math.min(facetSampleSize, maxCandidateWindow),
            SearchRequest.DEFAULT_SORT
        );
        return buildFacetsResponse(upstreamResponse.getContent());
    }

    private SearchRequest normalizeRequest(
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

        return new SearchRequest(
            normalizedTitle,
            normalizedCompany,
            normalizedLocation,
            normalizedEmploymentType,
            normalizedSalaryMin,
            normalizedSalaryMax,
            normalizedStatus == null ? DEFAULT_STATUS : normalizedStatus,
            validatedPage,
            cappedSize,
            validatedSort
        );
    }

    private PagedResponse<JobSearchResult> buildRankedResponse(SearchRequest request) {
        int candidateSize = Math.max((request.page() + 1) * request.size(), request.size() * 3);
        candidateSize = Math.max(candidateSize, 50);
        candidateSize = Math.min(candidateSize, maxCandidateWindow);

        PagedResponse<JobSearchResult> upstreamResponse = fetchUpstreamPage(request, 0, candidateSize, SearchRequest.DEFAULT_SORT);
        List<JobSearchResult> rankedContent = upstreamResponse.getContent().stream()
            .sorted(rankingComparator(request))
            .toList();

        return sliceResponse(rankedContent, upstreamResponse.getTotalElements(), request.page(), request.size());
    }

    private PagedResponse<JobSearchResult> fetchUpstreamPage(SearchRequest request, int page, int size, String sort) {
        SearchServiceException lastSearchException = null;
        int attempts = Math.max(1, retryMaxAttempts);

        for (int attempt = 1; attempt <= attempts; attempt++) {
            ensureCircuitClosed();

            try {
                URI uri = buildUri(request, page, size, sort);
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

                recordUpstreamSuccess();
                return response.getBody();
            } catch (SearchServiceException ex) {
                if (!isRetryable(ex) || attempt == attempts) {
                    recordUpstreamFailure(ex.getErrorCode());
                    throw ex;
                }
                lastSearchException = ex;
                sleepBeforeRetry();
            } catch (RestClientException ex) {
                if (attempt == attempts) {
                    break;
                }
                sleepBeforeRetry();
            }
        }

        recordUpstreamFailure("SEARCH_UPSTREAM_FAILED");
        if (lastSearchException != null) {
            throw lastSearchException;
        }
        throw new SearchServiceException(
            "SEARCH_UPSTREAM_FAILED",
            "Failed to fetch search results from job-service",
            502
        );
    }

    private URI buildUri(SearchRequest request, int page, int size, String sort) {
        return UriComponentsBuilder.fromUriString(jobServiceUrl)
            .path("/api/jobs/search")
            .queryParamIfPresent("title", optional(request.title()))
            .queryParamIfPresent("company", optional(request.company()))
            .queryParamIfPresent("location", optional(request.location()))
            .queryParamIfPresent("employmentType", optional(request.employmentType()))
            .queryParamIfPresent("salaryMin", optional(request.salaryMin()))
            .queryParamIfPresent("salaryMax", optional(request.salaryMax()))
            .queryParam("status", request.status())
            .queryParam("page", page)
            .queryParam("size", size)
            .queryParam("sort", sort)
            .build()
            .encode()
            .toUri();
    }

    private Comparator<JobSearchResult> rankingComparator(SearchRequest request) {
        return Comparator
            .comparingInt((JobSearchResult result) -> relevanceScore(result, request))
            .reversed()
            .thenComparing(JobSearchResult::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder()))
            .thenComparing(JobSearchResult::getId, Comparator.nullsLast(Comparator.naturalOrder()));
    }

    private int relevanceScore(JobSearchResult result, SearchRequest request) {
        int score = 0;
        score += exactOrPartialScore(result.getTitle(), request.title(), 120, 80);
        score += exactOrPartialScore(result.getCompany(), request.company(), 90, 60);
        score += exactOrPartialScore(result.getLocation(), request.location(), 70, 40);

        if (request.title() != null && containsNormalized(result.getDescription(), request.title())) {
            score += 25;
        }
        if ("ACTIVE".equalsIgnoreCase(result.getStatus())) {
            score += 25;
        }
        score += recencyScore(result.getCreatedAt());
        return score;
    }

    private int exactOrPartialScore(String candidate, String query, int exactScore, int partialScore) {
        if (query == null) {
            return 0;
        }
        if (equalsNormalized(candidate, query)) {
            return exactScore;
        }
        if (containsNormalized(candidate, query)) {
            return partialScore;
        }
        return 0;
    }

    private boolean equalsNormalized(String left, String right) {
        String normalizedLeft = normalizeText(left);
        return normalizedLeft != null && normalizedLeft.equalsIgnoreCase(right);
    }

    private boolean containsNormalized(String candidate, String query) {
        String normalizedCandidate = normalizeText(candidate);
        return normalizedCandidate != null
            && normalizedCandidate.toLowerCase(Locale.ROOT).contains(query.toLowerCase(Locale.ROOT));
    }

    private int recencyScore(LocalDateTime createdAt) {
        if (createdAt == null) {
            return 0;
        }

        long days = ChronoUnit.DAYS.between(createdAt, LocalDateTime.now());
        if (days <= 1) {
            return 35;
        }
        if (days <= 7) {
            return 25;
        }
        if (days <= 30) {
            return 15;
        }
        return 5;
    }

    private PagedResponse<JobSearchResult> sliceResponse(
            List<JobSearchResult> rankedContent,
            long totalElements,
            int page,
            int size) {
        int fromIndex = Math.min(page * size, rankedContent.size());
        int toIndex = Math.min(fromIndex + size, rankedContent.size());

        PagedResponse<JobSearchResult> response = new PagedResponse<>();
        response.setContent(rankedContent.subList(fromIndex, toIndex));
        response.setTotalElements(totalElements);
        response.setSize(size);
        response.setNumber(page);
        response.setTotalPages((int) Math.ceil((double) totalElements / size));
        response.setFirst(page == 0);
        response.setLast((long) (page + 1) * size >= totalElements);
        return response;
    }

    private JobSearchFacetsResponse buildFacetsResponse(List<JobSearchResult> content) {
        JobSearchFacetsResponse response = new JobSearchFacetsResponse();
        response.setLocations(topFacetValues(content, JobSearchResult::getLocation));
        response.setCompanies(topFacetValues(content, JobSearchResult::getCompany));
        response.setEmploymentTypes(topFacetValues(content, JobSearchResult::getEmploymentType));
        return response;
    }

    private List<FacetValueCount> topFacetValues(
            List<JobSearchResult> content,
            java.util.function.Function<JobSearchResult, String> extractor) {
        Map<String, Long> counts = new LinkedHashMap<>();

        for (JobSearchResult result : content) {
            String value = normalizeText(extractor.apply(result));
            if (value != null) {
                counts.merge(value, 1L, Long::sum);
            }
        }

        return counts.entrySet().stream()
            .sorted(Map.Entry.<String, Long>comparingByValue().reversed()
                .thenComparing(Map.Entry.comparingByKey(String.CASE_INSENSITIVE_ORDER)))
            .limit(facetMaxValues)
            .map(entry -> new FacetValueCount(entry.getKey(), entry.getValue()))
            .toList();
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
            return SearchRequest.DEFAULT_SORT;
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
        if ("SEARCH_CIRCUIT_OPEN".equals(ex.getErrorCode())) {
            return "circuit_open";
        }
        return ex.getHttpStatus() >= 500 ? "upstream_error" : "validation_error";
    }

    private void incrementRequestCounter(String outcome) {
        meterRegistry.counter("search.requests", "outcome", outcome).increment();
    }

    private void incrementUpstreamErrorCounter(String code) {
        meterRegistry.counter("search.upstream.errors", "code", code).increment();
    }

    private void incrementIndexFallbackCounter(String reason) {
        meterRegistry.counter("search.index.fallbacks", "reason", reason).increment();
    }

    private synchronized void ensureCircuitClosed() {
        if (circuitOpenedAt == null) {
            return;
        }

        if (Duration.between(circuitOpenedAt, Instant.now()).getSeconds() >= circuitOpenSeconds) {
            circuitOpenedAt = null;
            consecutiveUpstreamFailures.set(0);
            return;
        }

        throw new SearchServiceException(
            "SEARCH_CIRCUIT_OPEN",
            "Search is temporarily unavailable while upstream failures recover",
            503
        );
    }

    private synchronized void recordUpstreamSuccess() {
        consecutiveUpstreamFailures.set(0);
        circuitOpenedAt = null;
    }

    private synchronized void recordUpstreamFailure(String errorCode) {
        int failures = consecutiveUpstreamFailures.incrementAndGet();
        incrementUpstreamErrorCounter(errorCode);
        if (failures >= circuitFailureThreshold) {
            circuitOpenedAt = Instant.now();
        }
    }

    private boolean isRetryable(SearchServiceException ex) {
        return "SEARCH_UPSTREAM_EMPTY".equals(ex.getErrorCode());
    }

    private void sleepBeforeRetry() {
        if (retryBackoffMs <= 0) {
            return;
        }

        try {
            Thread.sleep(retryBackoffMs);
        } catch (InterruptedException interruptedException) {
            Thread.currentThread().interrupt();
            throw new SearchServiceException(
                "SEARCH_UPSTREAM_FAILED",
                "Search retry was interrupted",
                502
            );
        }
    }
}
