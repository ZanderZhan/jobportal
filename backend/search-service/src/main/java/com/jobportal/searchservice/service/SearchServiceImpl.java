package com.jobportal.searchservice.service;

import com.jobportal.searchservice.dto.AutocompleteSuggestion;
import com.jobportal.searchservice.dto.FacetValueCount;
import com.jobportal.searchservice.dto.JobAutocompleteResponse;
import com.jobportal.searchservice.dto.JobSearchFacetsResponse;
import com.jobportal.searchservice.dto.JobSearchResult;
import com.jobportal.searchservice.dto.PagedResponse;
import com.jobportal.searchservice.dto.SavedSearchRequest;
import com.jobportal.searchservice.dto.SavedSearchResponse;
import com.jobportal.searchservice.dto.SearchAbandonRequest;
import com.jobportal.searchservice.dto.SearchClickRequest;
import com.jobportal.searchservice.dto.SearchDiscoveryResponse;
import com.jobportal.searchservice.dto.SearchIndexStatusResponse;
import com.jobportal.searchservice.exception.SearchServiceException;
import com.jobportal.searchservice.repository.SavedSearchRepository;
import com.jobportal.searchservice.repository.SearchAnalyticsRepository;
import com.jobportal.searchservice.repository.SearchIndexRepository;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
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
    private static final Set<String> VALID_WORK_MODES = Set.of(
        "REMOTE",
        "HYBRID",
        "ONSITE"
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
    private final SavedSearchRepository savedSearchRepository;
    private final SearchAnalyticsRepository searchAnalyticsRepository;
    private final SearchCacheManager searchCacheManager;
    private final RestTemplate restTemplate;
    private final MeterRegistry meterRegistry;
    private final String jobServiceUrl;
    private final int maxPageSize;
    private final int maxCandidateWindow;
    private final int facetSampleSize;
    private final int facetMaxValues;
    private final int autocompleteLimit;
    private final int relatedSearchLimit;
    private final int retryMaxAttempts;
    private final long retryBackoffMs;
    private final int circuitFailureThreshold;
    private final long circuitOpenSeconds;
    private final AtomicInteger consecutiveUpstreamFailures = new AtomicInteger();
    private volatile Instant circuitOpenedAt;

    @Autowired
    public SearchServiceImpl(
            SearchIndexRepository searchIndexRepository,
            SavedSearchRepository savedSearchRepository,
            SearchAnalyticsRepository searchAnalyticsRepository,
            SearchCacheManager searchCacheManager,
            RestTemplate restTemplate,
            MeterRegistry meterRegistry,
            @Value("${services.job-service.url:http://localhost:8081}") String jobServiceUrl,
            @Value("${search.pagination.max-size:100}") int maxPageSize,
            @Value("${search.ranking.max-candidate-window:200}") int maxCandidateWindow,
            @Value("${search.facets.sample-size:200}") int facetSampleSize,
            @Value("${search.facets.max-values:10}") int facetMaxValues,
            @Value("${search.autocomplete.limit:8}") int autocompleteLimit,
            @Value("${search.discovery.related-limit:6}") int relatedSearchLimit,
            @Value("${search.resilience.retry.max-attempts:2}") int retryMaxAttempts,
            @Value("${search.resilience.retry.backoff-ms:100}") long retryBackoffMs,
            @Value("${search.resilience.circuit-breaker.failure-threshold:3}") int circuitFailureThreshold,
            @Value("${search.resilience.circuit-breaker.open-seconds:30}") long circuitOpenSeconds) {
        this.searchIndexRepository = searchIndexRepository;
        this.savedSearchRepository = savedSearchRepository;
        this.searchAnalyticsRepository = searchAnalyticsRepository;
        this.searchCacheManager = searchCacheManager;
        this.restTemplate = restTemplate;
        this.meterRegistry = meterRegistry;
        this.jobServiceUrl = jobServiceUrl;
        this.maxPageSize = maxPageSize;
        this.maxCandidateWindow = maxCandidateWindow;
        this.facetSampleSize = facetSampleSize;
        this.facetMaxValues = facetMaxValues;
        this.autocompleteLimit = autocompleteLimit;
        this.relatedSearchLimit = relatedSearchLimit;
        this.retryMaxAttempts = retryMaxAttempts;
        this.retryBackoffMs = retryBackoffMs;
        this.circuitFailureThreshold = circuitFailureThreshold;
        this.circuitOpenSeconds = circuitOpenSeconds;
    }

    @Override
    public PagedResponse<JobSearchResult> searchJobs(
            String userId,
            String sessionId,
            String title,
            String company,
            String location,
            String employmentType,
            String employmentTypes,
            BigDecimal salaryMin,
            BigDecimal salaryMax,
            String salaryCurrency,
            String workMode,
            Integer postedWithinDays,
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
                employmentTypes,
                salaryMin,
                salaryMax,
                salaryCurrency,
                workMode,
                postedWithinDays,
                status,
                page,
                size,
                sort
            );

            boolean canUseCache = normalizeText(userId) == null;
            if (canUseCache) {
                PagedResponse<JobSearchResult> cachedResponse = searchCacheManager.getSearch(request.cacheKey());
                if (cachedResponse != null) {
                    incrementRequestCounter("cache_hit");
                    return cachedResponse;
                }
            }

            List<SavedSearchResponse> personalizationSignals = loadPersonalizationSignals(userId);
            PagedResponse<JobSearchResult> response = searchViaIndexOrFallback(request, personalizationSignals);

            if (response.getTotalElements() == 0) {
                recordZeroResult(userId, sessionId, request);
            }
            if (canUseCache) {
                searchCacheManager.putSearch(request.cacheKey(), response);
            }

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
            String employmentTypes,
            BigDecimal salaryMin,
            BigDecimal salaryMax,
            String salaryCurrency,
            String workMode,
            Integer postedWithinDays,
            String status) {

        Timer.Sample sample = Timer.start(meterRegistry);
        String outcome = "success";

        try {
            SearchRequest request = normalizeRequest(
                title,
                company,
                location,
                employmentType,
                employmentTypes,
                salaryMin,
                salaryMax,
                salaryCurrency,
                workMode,
                postedWithinDays,
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

    @Override
    public JobAutocompleteResponse getJobAutocomplete(String query) {
        String normalizedQuery = normalizeText(query);
        JobAutocompleteResponse response = new JobAutocompleteResponse();
        if (normalizedQuery == null || normalizedQuery.length() < 2) {
            return response;
        }

        try {
            if (isIndexReady()) {
                response.setSuggestions(searchIndexRepository.autocomplete(normalizedQuery, autocompleteLimit));
                return response;
            }
        } catch (DataAccessException ex) {
            incrementIndexFallbackCounter("index_unavailable");
        }

        response.setSuggestions(buildAutocompleteFallback(normalizedQuery));
        return response;
    }

    @Override
    public SearchDiscoveryResponse getSearchDiscovery(
            String title,
            String company,
            String location,
            String employmentType,
            String employmentTypes,
            BigDecimal salaryMin,
            BigDecimal salaryMax,
            String salaryCurrency,
            String workMode,
            Integer postedWithinDays,
            String status) {

        SearchRequest request = normalizeRequest(
            title,
            company,
            location,
            employmentType,
            employmentTypes,
            salaryMin,
            salaryMax,
            salaryCurrency,
            workMode,
            postedWithinDays,
            status,
            0,
            Math.min(facetSampleSize, maxCandidateWindow),
            SearchRequest.DEFAULT_SORT
        );

        JobSearchFacetsResponse facetsResponse = facetsViaIndexOrFallback(request);
        SearchDiscoveryResponse response = new SearchDiscoveryResponse();
        response.setSuggestedLocations(facetsResponse.getLocations());
        response.setSuggestedCompanies(facetsResponse.getCompanies());
        response.setSuggestedEmploymentTypes(facetsResponse.getEmploymentTypes());
        response.setRelatedSearches(buildRelatedSearches(request));
        return response;
    }

    @Override
    public List<SavedSearchResponse> getSavedSearches(String userId) {
        return savedSearchRepository.findByUserId(requireUserId(userId));
    }

    @Override
    public SavedSearchResponse saveSearch(String userId, SavedSearchRequest request) {
        String normalizedUserId = requireUserId(userId);
        String normalizedTitle = normalizeText(request.getTitle());
        String normalizedCompany = normalizeText(request.getCompany());
        String normalizedLocation = normalizeText(request.getLocation());
        String normalizedEmploymentType = normalizeEnumValue(request.getEmploymentType());

        validateEnumValue("employmentType", normalizedEmploymentType, VALID_EMPLOYMENT_TYPES);

        if (normalizedTitle == null
                && normalizedCompany == null
                && normalizedLocation == null
                && normalizedEmploymentType == null) {
            throw invalidRequest("At least one saved search filter is required");
        }

        SavedSearchResponse savedSearch = new SavedSearchResponse();
        savedSearch.setName(resolveSavedSearchName(request.getName(), normalizedTitle, normalizedLocation, normalizedEmploymentType));
        savedSearch.setTitle(normalizedTitle);
        savedSearch.setCompany(normalizedCompany);
        savedSearch.setLocation(normalizedLocation);
        savedSearch.setEmploymentType(normalizedEmploymentType);
        return savedSearchRepository.save(normalizedUserId, savedSearch);
    }

    @Override
    public void deleteSavedSearch(String userId, Long id) {
        String normalizedUserId = requireUserId(userId);
        SavedSearchResponse existing = savedSearchRepository.findByIdAndUserId(id, normalizedUserId)
            .orElseThrow(() -> notFound("Saved search not found"));
        savedSearchRepository.deleteById(existing.getId());
    }

    @Override
    public void trackSearchClick(String userId, SearchClickRequest request) {
        String sessionId = normalizeText(request.getSessionId());
        if (sessionId == null) {
            throw invalidRequest("sessionId is required");
        }
        if (request.getJobId() == null) {
            throw invalidRequest("jobId is required");
        }
        searchAnalyticsRepository.recordClick(normalizeText(userId), sessionId, request.getJobId());
    }

    @Override
    public void trackSearchAbandon(String userId, SearchAbandonRequest request) {
        String sessionId = normalizeText(request.getSessionId());
        if (sessionId == null) {
            throw invalidRequest("sessionId is required");
        }
        searchAnalyticsRepository.recordAbandon(normalizeText(userId), sessionId);
    }

    private PagedResponse<JobSearchResult> searchViaIndexOrFallback(
            SearchRequest request,
            List<SavedSearchResponse> personalizationSignals) {
        try {
            SearchIndexStatusResponse indexStatus = searchIndexRepository.getStatus();
            if (indexStatus.isReady() && !indexStatus.isReindexInProgress()) {
                return searchIndexedDocuments(request, personalizationSignals);
            }
            incrementIndexFallbackCounter(indexStatus.isReindexInProgress() ? "reindex_in_progress" : "index_not_ready");
        } catch (DataAccessException ex) {
            incrementIndexFallbackCounter("index_unavailable");
        }

        return request.usesDefaultSort() && request.offset() < maxCandidateWindow
            ? buildRankedResponse(request, personalizationSignals)
            : fetchUpstreamPage(request, request.page(), request.size(), request.sort());
    }

    private PagedResponse<JobSearchResult> searchIndexedDocuments(
            SearchRequest request,
            List<SavedSearchResponse> personalizationSignals) {
        if (!request.usesDefaultSort() || request.offset() >= maxCandidateWindow) {
            return searchIndexRepository.search(request);
        }

        int candidateSize = Math.max((request.page() + 1) * request.size(), request.size() * 3);
        candidateSize = Math.max(candidateSize, 50);
        candidateSize = Math.min(candidateSize, maxCandidateWindow);

        PagedResponse<JobSearchResult> indexedResponse = searchIndexRepository.search(request.withPageAndSize(0, candidateSize));
        List<JobSearchResult> rankedContent = indexedResponse.getContent().stream()
            .sorted(rankingComparator(request, personalizationSignals))
            .toList();

        return sliceResponse(rankedContent, indexedResponse.getTotalElements(), request.page(), request.size());
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
            String employmentTypes,
            BigDecimal salaryMin,
            BigDecimal salaryMax,
            String salaryCurrency,
            String workMode,
            Integer postedWithinDays,
            String status,
            int page,
            int size,
            String sort) {

        String normalizedTitle = normalizeText(title);
        String normalizedCompany = normalizeText(company);
        String normalizedLocation = normalizeText(location);
        List<String> normalizedEmploymentTypes = normalizeEmploymentTypes(employmentType, employmentTypes);
        String normalizedStatus = normalizeEnumValue(status);
        BigDecimal normalizedSalaryMin = normalizeSalary(salaryMin, "salaryMin");
        BigDecimal normalizedSalaryMax = normalizeSalary(salaryMax, "salaryMax");
        String normalizedSalaryCurrency = normalizeCurrency(salaryCurrency);
        String normalizedWorkMode = normalizeEnumValue(workMode);
        Integer validatedPostedWithinDays = validatePostedWithinDays(postedWithinDays);
        int validatedPage = validatePage(page);
        int cappedSize = capPageSize(size);
        String validatedSort = normalizeSort(sort);

        validateEnumValues("employmentTypes", normalizedEmploymentTypes, VALID_EMPLOYMENT_TYPES);
        validateEnumValue("status", normalizedStatus, VALID_STATUSES);
        validateEnumValue("workMode", normalizedWorkMode, VALID_WORK_MODES);
        validateSalaryRange(normalizedSalaryMin, normalizedSalaryMax);

        return new SearchRequest(
            normalizedTitle,
            normalizedCompany,
            normalizedLocation,
            normalizedEmploymentTypes,
            normalizedSalaryMin,
            normalizedSalaryMax,
            normalizedSalaryCurrency,
            normalizedWorkMode,
            validatedPostedWithinDays,
            normalizedStatus == null ? DEFAULT_STATUS : normalizedStatus,
            validatedPage,
            cappedSize,
            validatedSort
        );
    }

    private PagedResponse<JobSearchResult> buildRankedResponse(
            SearchRequest request,
            List<SavedSearchResponse> personalizationSignals) {
        int candidateSize = Math.max((request.page() + 1) * request.size(), request.size() * 3);
        candidateSize = Math.max(candidateSize, 50);
        candidateSize = Math.min(candidateSize, maxCandidateWindow);

        PagedResponse<JobSearchResult> upstreamResponse = fetchUpstreamPage(request, 0, candidateSize, SearchRequest.DEFAULT_SORT);
        List<JobSearchResult> rankedContent = upstreamResponse.getContent().stream()
            .sorted(rankingComparator(request, personalizationSignals))
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
                    new org.springframework.core.ParameterizedTypeReference<>() {}
                );

                if (response.getBody() == null) {
                    throw new SearchServiceException(
                        "SEARCH_UPSTREAM_EMPTY",
                        "Job search returned an empty response",
                        502
                    );
                }

                recordUpstreamSuccess();
                return applyFallbackFilters(response.getBody(), request, page, size);
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
            .queryParamIfPresent("employmentType", optional(request.primaryEmploymentType()))
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

    private Comparator<JobSearchResult> rankingComparator(
            SearchRequest request,
            List<SavedSearchResponse> personalizationSignals) {
        return Comparator
            .comparingInt((JobSearchResult result) -> relevanceScore(result, request, personalizationSignals))
            .reversed()
            .thenComparing(JobSearchResult::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder()))
            .thenComparing(JobSearchResult::getId, Comparator.nullsLast(Comparator.naturalOrder()));
    }

    private int relevanceScore(
            JobSearchResult result,
            SearchRequest request,
            List<SavedSearchResponse> personalizationSignals) {
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
        score += personalizationScore(result, personalizationSignals);
        score += recencyScore(result.getCreatedAt());
        return score;
    }

    private int personalizationScore(JobSearchResult result, List<SavedSearchResponse> savedSearches) {
        if (savedSearches.isEmpty()) {
            return 0;
        }

        int bestScore = 0;
        for (SavedSearchResponse savedSearch : savedSearches) {
            int score = 0;
            score += exactOrPartialScore(result.getTitle(), savedSearch.getTitle(), 90, 60);
            score += exactOrPartialScore(result.getCompany(), savedSearch.getCompany(), 70, 45);
            score += exactOrPartialScore(result.getLocation(), savedSearch.getLocation(), 60, 35);
            if (savedSearch.getEmploymentType() != null
                    && savedSearch.getEmploymentType().equalsIgnoreCase(result.getEmploymentType())) {
                score += 50;
            }
            bestScore = Math.max(bestScore, score);
        }
        return bestScore;
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

    private List<AutocompleteSuggestion> buildAutocompleteFallback(String query) {
        SearchRequest request = new SearchRequest(
            query,
            null,
            null,
            List.of(),
            null,
            null,
            null,
            null,
            null,
            DEFAULT_STATUS,
            0,
            Math.min(autocompleteLimit, maxCandidateWindow),
            SearchRequest.DEFAULT_SORT
        );

        List<JobSearchResult> content = fetchUpstreamPage(
            request,
            0,
            Math.min(autocompleteLimit, maxCandidateWindow),
            SearchRequest.DEFAULT_SORT
        ).getContent();

        LinkedHashMap<String, AutocompleteSuggestion> suggestions = new LinkedHashMap<>();
        for (JobSearchResult result : content) {
            addSuggestion(suggestions, result.getTitle(), "TITLE", query);
            addSuggestion(suggestions, result.getCompany(), "COMPANY", query);
            addSuggestion(suggestions, result.getLocation(), "LOCATION", query);
            if (suggestions.size() >= autocompleteLimit) {
                break;
            }
        }
        return new ArrayList<>(suggestions.values());
    }

    private void addSuggestion(
            Map<String, AutocompleteSuggestion> suggestions,
            String value,
            String type,
            String query) {
        String normalizedValue = normalizeText(value);
        if (normalizedValue == null
                || !normalizedValue.toLowerCase(Locale.ROOT).contains(query.toLowerCase(Locale.ROOT))) {
            return;
        }

        suggestions.compute(normalizedValue + "|" + type, (key, existing) -> {
            if (existing == null) {
                return new AutocompleteSuggestion(normalizedValue, type, 1);
            }
            existing.setCount(existing.getCount() + 1);
            return existing;
        });
    }

    private List<String> buildRelatedSearches(SearchRequest request) {
        LinkedHashSet<String> related = new LinkedHashSet<>();

        String seedQuery = firstNonNull(request.title(), request.company(), request.location());
        if (seedQuery != null) {
            List<AutocompleteSuggestion> suggestions = getJobAutocomplete(seedQuery).getSuggestions();
            for (AutocompleteSuggestion suggestion : suggestions) {
                if (!suggestion.getValue().equalsIgnoreCase(seedQuery)) {
                    related.add(suggestion.getValue());
                }
                if (related.size() >= relatedSearchLimit) {
                    break;
                }
            }
        }

        if (related.size() < relatedSearchLimit) {
            JobSearchFacetsResponse facets = facetsViaIndexOrFallback(request);
            addFacetRelated(related, facets.getLocations());
            addFacetRelated(related, facets.getCompanies());
        }

        return related.stream()
            .limit(relatedSearchLimit)
            .toList();
    }

    private void addFacetRelated(LinkedHashSet<String> related, List<FacetValueCount> values) {
        for (FacetValueCount value : values) {
            related.add(value.getValue());
            if (related.size() >= relatedSearchLimit) {
                return;
            }
        }
    }

    private List<SavedSearchResponse> loadPersonalizationSignals(String userId) {
        String normalizedUserId = normalizeText(userId);
        if (normalizedUserId == null) {
            return List.of();
        }
        return savedSearchRepository.findByUserId(normalizedUserId);
    }

    private void recordZeroResult(String userId, String sessionId, SearchRequest request) {
        try {
            searchAnalyticsRepository.recordZeroResult(normalizeText(userId), normalizeText(sessionId), request);
        } catch (DataAccessException ex) {
            meterRegistry.counter("search.analytics.failures", "type", "zero_result").increment();
        }
    }

    private String requireUserId(String userId) {
        String normalizedUserId = normalizeText(userId);
        if (normalizedUserId == null) {
            throw new SearchServiceException(
                "SEARCH_AUTH_REQUIRED",
                "A search user id is required for this operation",
                401
            );
        }
        return normalizedUserId;
    }

    private String resolveSavedSearchName(
            String name,
            String title,
            String location,
            String employmentType) {
        String normalizedName = normalizeText(name);
        if (normalizedName != null) {
            return normalizedName;
        }

        List<String> parts = new ArrayList<>();
        if (title != null) {
            parts.add(title);
        }
        if (location != null) {
            parts.add(location);
        }
        if (employmentType != null) {
            parts.add(employmentType.replace('_', ' '));
        }

        if (parts.isEmpty()) {
            return "Saved search";
        }
        return String.join(" · ", parts);
    }

    private boolean isIndexReady() {
        SearchIndexStatusResponse status = searchIndexRepository.getStatus();
        return status.isReady() && !status.isReindexInProgress();
    }

    private String firstNonNull(String... values) {
        for (String value : values) {
            String normalizedValue = normalizeText(value);
            if (normalizedValue != null) {
                return normalizedValue;
            }
        }
        return null;
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

    private List<String> normalizeEmploymentTypes(String employmentType, String employmentTypes) {
        LinkedHashSet<String> normalized = new LinkedHashSet<>();

        String singleValue = normalizeEnumValue(employmentType);
        if (singleValue != null) {
            normalized.add(singleValue);
        }

        String normalizedList = normalizeText(employmentTypes);
        if (normalizedList != null) {
            for (String value : normalizedList.split(",")) {
                String normalizedValue = normalizeEnumValue(value);
                if (normalizedValue != null) {
                    normalized.add(normalizedValue);
                }
            }
        }

        return List.copyOf(normalized);
    }

    private String normalizeCurrency(String currency) {
        String normalized = normalizeText(currency);
        if (normalized == null) {
            return null;
        }

        String upperCaseCurrency = normalized.toUpperCase(Locale.ROOT);
        if (!upperCaseCurrency.matches("[A-Z]{3}")) {
            throw invalidRequest("salaryCurrency must be a 3-letter ISO currency code");
        }

        return upperCaseCurrency;
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

    private Integer validatePostedWithinDays(Integer postedWithinDays) {
        if (postedWithinDays == null) {
            return null;
        }

        if (postedWithinDays < 1) {
            throw invalidRequest("postedWithinDays must be greater than or equal to 1");
        }

        if (postedWithinDays > 3650) {
            throw invalidRequest("postedWithinDays must be less than or equal to 3650");
        }

        return postedWithinDays;
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

    private void validateEnumValues(String fieldName, List<String> values, Set<String> allowedValues) {
        for (String value : values) {
            validateEnumValue(fieldName, value, allowedValues);
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

    private SearchServiceException notFound(String message) {
        return new SearchServiceException(
            "SEARCH_NOT_FOUND",
            message,
            404
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

    private PagedResponse<JobSearchResult> applyFallbackFilters(
            PagedResponse<JobSearchResult> response,
            SearchRequest request,
            int page,
            int size) {
        if (!hasAdditionalFiltersForFallback(request)) {
            return response;
        }

        List<JobSearchResult> filteredContent = response.getContent().stream()
            .filter(result -> matchesAdvancedFilters(result, request))
            .toList();

        PagedResponse<JobSearchResult> filteredResponse = new PagedResponse<>();
        filteredResponse.setContent(filteredContent);
        filteredResponse.setTotalElements(filteredContent.size());
        filteredResponse.setSize(size);
        filteredResponse.setNumber(page);
        filteredResponse.setTotalPages(filteredContent.isEmpty() ? 0 : 1);
        filteredResponse.setFirst(page == 0);
        filteredResponse.setLast(true);
        return filteredResponse;
    }

    private boolean hasAdditionalFiltersForFallback(SearchRequest request) {
        return request.salaryCurrency() != null
            || request.workMode() != null
            || request.postedWithinDays() != null
            || request.employmentTypes().size() > 1;
    }

    private boolean matchesAdvancedFilters(JobSearchResult result, SearchRequest request) {
        if (request.hasEmploymentTypeFilter()) {
            String employmentType = normalizeEnumValue(result.getEmploymentType());
            if (employmentType == null || !request.employmentTypes().contains(employmentType)) {
                return false;
            }
        }

        if (request.salaryCurrency() != null) {
            String salaryCurrency = normalizeCurrency(result.getSalaryCurrency());
            if (!request.salaryCurrency().equals(salaryCurrency)) {
                return false;
            }
        }

        if (request.postedAfter() != null) {
            LocalDateTime createdAt = result.getCreatedAt();
            if (createdAt == null || createdAt.isBefore(request.postedAfter())) {
                return false;
            }
        }

        if (request.workMode() != null && !matchesWorkMode(result.getLocation(), request.workMode())) {
            return false;
        }

        return true;
    }

    private boolean matchesWorkMode(String location, String workMode) {
        String normalizedLocation = normalizeText(location);
        if (normalizedLocation == null) {
            return false;
        }

        String lowerLocation = normalizedLocation.toLowerCase(Locale.ROOT);
        return switch (workMode) {
            case "REMOTE" -> lowerLocation.contains("remote");
            case "HYBRID" -> lowerLocation.contains("hybrid");
            case "ONSITE" -> !lowerLocation.contains("remote") && !lowerLocation.contains("hybrid");
            default -> false;
        };
    }
}
