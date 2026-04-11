package com.jobportal.searchservice.service;

import com.jobportal.searchservice.dto.AutocompleteSuggestion;
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
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.net.URI;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SearchServiceTest {

    @Mock
    private SearchIndexRepository searchIndexRepository;

    @Mock
    private SavedSearchRepository savedSearchRepository;

    @Mock
    private SearchAnalyticsRepository searchAnalyticsRepository;

    @Mock
    private SearchCacheManager searchCacheManager;

    @Mock
    private RestTemplate restTemplate;

    private SimpleMeterRegistry meterRegistry;
    private SearchServiceImpl searchService;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        searchService = new SearchServiceImpl(
            searchIndexRepository,
            savedSearchRepository,
            searchAnalyticsRepository,
            searchCacheManager,
            restTemplate,
            meterRegistry,
            "http://job-service:8081",
            100,
            200,
            200,
            10,
            8,
            6,
            2,
            0,
            2,
            30
        );
    }

    @Test
    void searchJobs_ShouldUseIndexedBackendAndPersonalizeForSavedSearches() {
        SearchIndexStatusResponse status = readyStatus();
        JobSearchResult generic = createJob(2L, "Backend Engineer", "Acme", "Dublin", "FULL_TIME", "ACTIVE", 2);
        JobSearchResult personalized = createJob(1L, "Frontend Engineer", "Northwind", "Remote", "FULL_TIME", "ACTIVE", 5);

        PagedResponse<JobSearchResult> indexedResponse = new PagedResponse<>();
        indexedResponse.setContent(List.of(generic, personalized));
        indexedResponse.setTotalElements(2);
        indexedResponse.setSize(100);
        indexedResponse.setNumber(0);
        indexedResponse.setTotalPages(1);
        indexedResponse.setFirst(true);
        indexedResponse.setLast(true);

        SavedSearchResponse savedSearch = new SavedSearchResponse();
        savedSearch.setId(1L);
        savedSearch.setName("Remote Frontend");
        savedSearch.setTitle("Frontend");
        savedSearch.setLocation("Remote");
        savedSearch.setEmploymentType("FULL_TIME");
        savedSearch.setCreatedAt(Instant.now());
        savedSearch.setUpdatedAt(Instant.now());

        when(searchIndexRepository.getStatus()).thenReturn(status);
        when(searchIndexRepository.search(any())).thenReturn(indexedResponse);
        when(savedSearchRepository.findByUserId("user-1")).thenReturn(List.of(savedSearch));

        PagedResponse<JobSearchResult> actual = searchService.searchJobs(
            "user-1",
            "session-1",
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            0,
            20,
            "createdAt,desc"
        );

        assertEquals(2, actual.getTotalElements());
        assertEquals("Frontend Engineer", actual.getContent().get(0).getTitle());
        verify(searchIndexRepository).search(any());
        verify(searchCacheManager, never()).putSearch(any(), any());
    }

    @Test
    void searchJobs_WhenAnonymous_ShouldUseCache() {
        SearchIndexStatusResponse status = readyStatus();
        JobSearchResult result = createJob(1L, "Software Engineer", "Northwind", "Dublin", "FULL_TIME", "ACTIVE", 2);

        PagedResponse<JobSearchResult> indexedResponse = new PagedResponse<>();
        indexedResponse.setContent(List.of(result));
        indexedResponse.setTotalElements(1);
        indexedResponse.setSize(20);
        indexedResponse.setNumber(0);
        indexedResponse.setTotalPages(1);
        indexedResponse.setFirst(true);
        indexedResponse.setLast(true);

        when(searchCacheManager.getSearch(any())).thenReturn(null, indexedResponse);
        when(searchIndexRepository.getStatus()).thenReturn(status);
        when(searchIndexRepository.search(any())).thenReturn(indexedResponse);

        searchService.searchJobs(
            null,
            "session-1",
            "Software Engineer",
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            0,
            20,
            "createdAt,desc"
        );

        PagedResponse<JobSearchResult> cached = searchService.searchJobs(
            null,
            "session-2",
            "Software Engineer",
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            0,
            20,
            "createdAt,desc"
        );

        assertEquals(1, cached.getTotalElements());
        verify(searchCacheManager).putSearch(any(), any(PagedResponse.class));
    }

    @Test
    void searchJobs_WhenResultsAreEmpty_ShouldRecordZeroResultAnalytics() {
        SearchIndexStatusResponse status = readyStatus();
        PagedResponse<JobSearchResult> indexedResponse = new PagedResponse<>();
        indexedResponse.setContent(List.of());
        indexedResponse.setTotalElements(0);
        indexedResponse.setSize(20);
        indexedResponse.setNumber(0);
        indexedResponse.setTotalPages(0);
        indexedResponse.setFirst(true);
        indexedResponse.setLast(true);

        when(searchCacheManager.getSearch(any())).thenReturn(null);
        when(searchIndexRepository.getStatus()).thenReturn(status);
        when(searchIndexRepository.search(any())).thenReturn(indexedResponse);

        searchService.searchJobs(
            null,
            "session-1",
            "Ghost Job",
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            0,
            20,
            "createdAt,desc"
        );

        verify(searchAnalyticsRepository).recordZeroResult(eq(null), eq("session-1"), any(SearchRequest.class));
    }

    @Test
    void getJobAutocomplete_ShouldUseIndexSuggestions() {
        SearchIndexStatusResponse status = readyStatus();
        when(searchIndexRepository.getStatus()).thenReturn(status);
        when(searchIndexRepository.autocomplete("software", 8))
            .thenReturn(List.of(new AutocompleteSuggestion("Software Engineer", "TITLE", 3)));

        JobAutocompleteResponse response = searchService.getJobAutocomplete("software");

        assertEquals(1, response.getSuggestions().size());
        assertEquals("Software Engineer", response.getSuggestions().get(0).getValue());
    }

    @Test
    void getSearchDiscovery_ShouldReturnFacetsAndRelatedSearches() {
        SearchIndexStatusResponse status = readyStatus();
        JobSearchFacetsResponse facets = new JobSearchFacetsResponse();
        facets.setLocations(List.of(new com.jobportal.searchservice.dto.FacetValueCount("Remote", 2)));
        facets.setCompanies(List.of(new com.jobportal.searchservice.dto.FacetValueCount("Northwind", 2)));
        facets.setEmploymentTypes(List.of(new com.jobportal.searchservice.dto.FacetValueCount("FULL_TIME", 2)));

        when(searchIndexRepository.getStatus()).thenReturn(status);
        when(searchIndexRepository.getFacets(any(), eq(10))).thenReturn(facets);
        when(searchIndexRepository.autocomplete("Engineer", 8))
            .thenReturn(List.of(new AutocompleteSuggestion("Backend Engineer", "TITLE", 2)));

        SearchDiscoveryResponse response = searchService.getSearchDiscovery(
            "Engineer",
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null
        );

        assertEquals("Backend Engineer", response.getRelatedSearches().get(0));
        assertEquals("Remote", response.getSuggestedLocations().get(0).getValue());
    }

    @Test
    void saveSearch_ShouldNormalizeAndPersistSavedSearch() {
        SavedSearchRequest request = new SavedSearchRequest();
        request.setTitle("  Frontend Engineer ");
        request.setLocation(" Remote ");
        request.setEmploymentType("full-time");

        SavedSearchResponse saved = new SavedSearchResponse();
        saved.setId(1L);
        saved.setName("Frontend Engineer · Remote · FULL TIME");

        when(savedSearchRepository.save(eq("user-1"), any(SavedSearchResponse.class))).thenReturn(saved);

        SavedSearchResponse response = searchService.saveSearch("user-1", request);

        assertEquals(1L, response.getId());
        verify(savedSearchRepository).save(eq("user-1"), any(SavedSearchResponse.class));
    }

    @Test
    void deleteSavedSearch_ShouldRequireExistingSavedSearch() {
        when(savedSearchRepository.findByIdAndUserId(5L, "user-1")).thenReturn(java.util.Optional.empty());

        SearchServiceException exception = assertThrows(SearchServiceException.class, () -> searchService.deleteSavedSearch("user-1", 5L));

        assertEquals("SEARCH_NOT_FOUND", exception.getErrorCode());
    }

    @Test
    void trackSearchClick_ShouldValidatePayload() {
        SearchClickRequest request = new SearchClickRequest();
        request.setSessionId("session-1");
        request.setJobId(99L);

        searchService.trackSearchClick("user-1", request);

        verify(searchAnalyticsRepository).recordClick("user-1", "session-1", 99L);
    }

    @Test
    void trackSearchAbandon_ShouldValidatePayload() {
        SearchAbandonRequest request = new SearchAbandonRequest();
        request.setSessionId("session-1");

        searchService.trackSearchAbandon("user-1", request);

        verify(searchAnalyticsRepository).recordAbandon("user-1", "session-1");
    }

    @Test
    void searchJobs_WhenIndexIsUnavailable_ShouldFallbackToUpstream() {
        JobSearchResult result = createJob(1L, "Software Engineer", "Northwind", "Dublin", "FULL_TIME", "ACTIVE", 2);
        PagedResponse<JobSearchResult> upstreamResponse = new PagedResponse<>();
        upstreamResponse.setContent(List.of(result));
        upstreamResponse.setTotalElements(1);

        when(searchCacheManager.getSearch(any())).thenReturn(null);
        when(searchIndexRepository.getStatus()).thenThrow(new DataAccessResourceFailureException("index down"));
        when(restTemplate.exchange(
                any(URI.class),
                eq(HttpMethod.GET),
                eq(HttpEntity.EMPTY),
                any(org.springframework.core.ParameterizedTypeReference.class)))
            .thenReturn(ResponseEntity.ok(upstreamResponse));

        PagedResponse<JobSearchResult> actual = searchService.searchJobs(
            null,
            "session-1",
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            0,
            20,
            "title,asc"
        );

        assertEquals(1, actual.getTotalElements());
    }

    @Test
    void searchJobs_ShouldNormalizeAndPassAdvancedFiltersToIndex() {
        SearchIndexStatusResponse status = readyStatus();
        PagedResponse<JobSearchResult> indexedResponse = new PagedResponse<>();
        indexedResponse.setContent(List.of());
        indexedResponse.setTotalElements(0);
        indexedResponse.setSize(20);
        indexedResponse.setNumber(0);
        indexedResponse.setTotalPages(0);
        indexedResponse.setFirst(true);
        indexedResponse.setLast(true);

        when(searchCacheManager.getSearch(any())).thenReturn(null);
        when(searchIndexRepository.getStatus()).thenReturn(status);
        when(searchIndexRepository.search(any())).thenReturn(indexedResponse);

        searchService.searchJobs(
            null,
            "session-1",
            null,
            null,
            null,
            null,
            " full-time , contract ",
            new BigDecimal("50000"),
            new BigDecimal("90000"),
            "eur",
            "remote",
            30,
            "active",
            0,
            20,
            "createdAt,desc"
        );

        verify(searchIndexRepository).search(argThat(request ->
            request.employmentTypes().equals(List.of("FULL_TIME", "CONTRACT"))
                && new BigDecimal("50000").compareTo(request.salaryMin()) == 0
                && new BigDecimal("90000").compareTo(request.salaryMax()) == 0
                && "EUR".equals(request.salaryCurrency())
                && "REMOTE".equals(request.workMode())
                && Integer.valueOf(30).equals(request.postedWithinDays())
                && "ACTIVE".equals(request.status())
        ));
    }

    @Test
    void searchJobs_WhenUpstreamFails_ShouldThrowException() {
        SearchIndexStatusResponse status = new SearchIndexStatusResponse();
        status.setReady(false);
        when(searchCacheManager.getSearch(any())).thenReturn(null);
        when(searchIndexRepository.getStatus()).thenReturn(status);
        when(restTemplate.exchange(
                any(URI.class),
                eq(HttpMethod.GET),
                eq(HttpEntity.EMPTY),
                any(org.springframework.core.ParameterizedTypeReference.class)))
            .thenThrow(new RestClientException("upstream failed"));

        SearchServiceException exception = assertThrows(SearchServiceException.class, () -> searchService.searchJobs(
            null,
            "session-1",
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            0,
            20,
            "createdAt,desc"
        ));

        assertEquals("SEARCH_UPSTREAM_FAILED", exception.getErrorCode());
    }

    @Test
    void searchJobs_WhenPageSizeIsBelowMinimum_ShouldRejectBeforeCallingBackends() {
        SearchServiceException exception = assertThrows(SearchServiceException.class, () -> searchService.searchJobs(
            null,
            "session-1",
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            0,
            0,
            "createdAt,desc"
        ));

        assertEquals("SEARCH_INVALID_REQUEST", exception.getErrorCode());
        verifyNoInteractions(searchIndexRepository);
    }

    @Test
    void searchJobs_WhenPostedWindowIsInvalid_ShouldRejectBeforeCallingBackends() {
        SearchServiceException exception = assertThrows(SearchServiceException.class, () -> searchService.searchJobs(
            null,
            "session-1",
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            0,
            null,
            0,
            20,
            "createdAt,desc"
        ));

        assertEquals("SEARCH_INVALID_REQUEST", exception.getErrorCode());
        verifyNoInteractions(searchIndexRepository);
    }

    private SearchIndexStatusResponse readyStatus() {
        SearchIndexStatusResponse status = new SearchIndexStatusResponse();
        status.setReady(true);
        status.setReindexInProgress(false);
        status.setDocumentCount(1);
        return status;
    }

    private JobSearchResult createJob(
            Long id,
            String title,
            String company,
            String location,
            String employmentType,
            String status,
            long daysOld) {
        JobSearchResult result = new JobSearchResult();
        result.setId(id);
        result.setTitle(title);
        result.setDescription("Relevant description for " + title);
        result.setCompany(company);
        result.setLocation(location);
        result.setEmploymentType(employmentType);
        result.setStatus(status);
        result.setSalaryMin(new BigDecimal("50000"));
        result.setSalaryMax(new BigDecimal("70000"));
        result.setSalaryCurrency("EUR");
        result.setCreatedAt(LocalDateTime.now().minusDays(daysOld));
        result.setUpdatedAt(LocalDateTime.now().minusDays(daysOld));
        return result;
    }
}
