package com.jobportal.searchservice.service;

import com.jobportal.searchservice.dto.JobSearchFacetsResponse;
import com.jobportal.searchservice.dto.JobSearchResult;
import com.jobportal.searchservice.dto.PagedResponse;
import com.jobportal.searchservice.dto.SearchIndexStatusResponse;
import com.jobportal.searchservice.exception.SearchServiceException;
import com.jobportal.searchservice.repository.SearchIndexRepository;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.net.URI;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
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
            searchCacheManager,
            restTemplate,
            meterRegistry,
            "http://job-service:8081",
            100,
            200,
            200,
            10,
            2,
            0,
            2,
            30
        );
    }

    @Test
    void searchJobs_ShouldUseIndexedBackendAndCacheResults() {
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

        PagedResponse<JobSearchResult> actual = searchService.searchJobs(
            "  Software   Engineer  ",
            null,
            " Dublin ",
            null,
            null,
            null,
            null,
            0,
            20,
            "createdAt,desc"
        );
        PagedResponse<JobSearchResult> cached = searchService.searchJobs(
            "Software Engineer",
            null,
            "Dublin",
            null,
            null,
            null,
            null,
            0,
            20,
            "createdAt,desc"
        );

        assertEquals(1, actual.getTotalElements());
        assertEquals("Software Engineer", actual.getContent().get(0).getTitle());
        assertEquals(1, cached.getTotalElements());
        verify(searchIndexRepository, times(1)).search(any());
        verify(searchCacheManager, times(1)).putSearch(any(), eq(indexedResponse));
        verifyNoInteractions(restTemplate);
        assertEquals(1.0, meterRegistry.get("search.requests").tag("outcome", "success").counter().count());
        assertEquals(1.0, meterRegistry.get("search.requests").tag("outcome", "cache_hit").counter().count());
    }

    @Test
    void searchJobs_WhenIndexIsNotReady_ShouldFallbackToUpstreamAndApplyRanking() {
        SearchIndexStatusResponse status = new SearchIndexStatusResponse();
        status.setReady(false);
        status.setReindexInProgress(false);

        JobSearchResult partialMatch = createJob(2L, "Engineer", "Acme", "Limerick", "FULL_TIME", "ACTIVE", 5);
        JobSearchResult exactMatch = createJob(1L, "Software Engineer", "Northwind", "Dublin", "FULL_TIME", "ACTIVE", 10);

        PagedResponse<JobSearchResult> upstreamResponse = new PagedResponse<>();
        upstreamResponse.setContent(List.of(partialMatch, exactMatch));
        upstreamResponse.setTotalElements(2);

        when(searchCacheManager.getSearch(any())).thenReturn(null);
        when(searchIndexRepository.getStatus()).thenReturn(status);
        when(restTemplate.exchange(
                any(URI.class),
                eq(HttpMethod.GET),
                eq(HttpEntity.EMPTY),
                any(ParameterizedTypeReference.class)))
            .thenReturn(ResponseEntity.ok(upstreamResponse));

        PagedResponse<JobSearchResult> actual = searchService.searchJobs(
            "Software Engineer",
            null,
            "Limerick",
            "FULL_TIME",
            new BigDecimal("50000"),
            new BigDecimal("70000"),
            "ACTIVE",
            0,
            100,
            "createdAt,desc"
        );

        ArgumentCaptor<URI> uriCaptor = ArgumentCaptor.forClass(URI.class);
        verify(restTemplate).exchange(
            uriCaptor.capture(),
            eq(HttpMethod.GET),
            eq(HttpEntity.EMPTY),
            any(ParameterizedTypeReference.class)
        );

        String uri = uriCaptor.getValue().toString();
        assertTrue(uri.contains("/api/jobs/search"));
        assertTrue(uri.contains("size=200"));
        assertEquals("Software Engineer", actual.getContent().get(0).getTitle());
        assertEquals(1.0, meterRegistry.get("search.index.fallbacks").tag("reason", "index_not_ready").counter().count());
    }

    @Test
    void getJobSearchFacets_WhenIndexIsReady_ShouldUseIndexedFacets() {
        SearchIndexStatusResponse status = readyStatus();
        JobSearchFacetsResponse facets = new JobSearchFacetsResponse();
        facets.setLocations(List.of(new com.jobportal.searchservice.dto.FacetValueCount("Dublin", 2)));
        facets.setCompanies(List.of(new com.jobportal.searchservice.dto.FacetValueCount("Northwind", 2)));
        facets.setEmploymentTypes(List.of(new com.jobportal.searchservice.dto.FacetValueCount("FULL_TIME", 2)));

        when(searchCacheManager.getFacets(any())).thenReturn(null);
        when(searchIndexRepository.getStatus()).thenReturn(status);
        when(searchIndexRepository.getFacets(any(), eq(10))).thenReturn(facets);

        JobSearchFacetsResponse actual = searchService.getJobSearchFacets(
            null,
            null,
            null,
            null,
            null,
            null,
            null
        );

        assertEquals("Dublin", actual.getLocations().get(0).getValue());
        verify(searchIndexRepository).getFacets(any(), eq(10));
        verifyNoInteractions(restTemplate);
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
                any(ParameterizedTypeReference.class)))
            .thenReturn(ResponseEntity.ok(upstreamResponse));

        PagedResponse<JobSearchResult> actual = searchService.searchJobs(
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
        assertEquals(1.0, meterRegistry.get("search.index.fallbacks").tag("reason", "index_unavailable").counter().count());
    }

    @Test
    void searchJobs_WhenRequestIsInvalid_ShouldRejectBeforeCallingBackends() {
        SearchServiceException exception = assertThrows(SearchServiceException.class, () -> searchService.searchJobs(
            null,
            null,
            null,
            "invalid-type",
            null,
            null,
            null,
            0,
            20,
            "createdAt,desc"
        ));

        assertEquals("SEARCH_INVALID_REQUEST", exception.getErrorCode());
        verifyNoInteractions(searchIndexRepository, restTemplate);
    }

    @Test
    void searchJobs_WhenUpstreamBodyIsEmpty_ShouldThrowException() {
        SearchIndexStatusResponse status = new SearchIndexStatusResponse();
        status.setReady(false);
        when(searchCacheManager.getSearch(any())).thenReturn(null);
        when(searchIndexRepository.getStatus()).thenReturn(status);
        when(restTemplate.exchange(
                any(URI.class),
                eq(HttpMethod.GET),
                eq(HttpEntity.EMPTY),
                any(ParameterizedTypeReference.class)))
            .thenReturn(ResponseEntity.ok(null));

        SearchServiceException exception = assertThrows(SearchServiceException.class, () -> searchService.searchJobs(
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

        assertEquals("SEARCH_UPSTREAM_EMPTY", exception.getErrorCode());
        assertEquals(1.0, meterRegistry.get("search.upstream.errors").tag("code", "SEARCH_UPSTREAM_EMPTY").counter().count());
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
                any(ParameterizedTypeReference.class)))
            .thenThrow(new RestClientException("upstream failed"));

        SearchServiceException exception = assertThrows(SearchServiceException.class, () -> searchService.searchJobs(
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

        assertInstanceOf(SearchServiceException.class, exception);
        assertEquals("SEARCH_UPSTREAM_FAILED", exception.getErrorCode());
        assertEquals(1.0, meterRegistry.get("search.upstream.errors").tag("code", "SEARCH_UPSTREAM_FAILED").counter().count());
    }

    @Test
    void searchJobs_WhenCircuitBreakerOpens_ShouldRejectSubsequentRequests() {
        SearchIndexStatusResponse status = new SearchIndexStatusResponse();
        status.setReady(false);
        when(searchCacheManager.getSearch(any())).thenReturn(null);
        when(searchIndexRepository.getStatus()).thenReturn(status);
        when(restTemplate.exchange(
                any(URI.class),
                eq(HttpMethod.GET),
                eq(HttpEntity.EMPTY),
                any(ParameterizedTypeReference.class)))
            .thenThrow(new RestClientException("upstream failed"));

        assertThrows(SearchServiceException.class, () -> searchService.searchJobs(
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

        assertThrows(SearchServiceException.class, () -> searchService.searchJobs(
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

        SearchServiceException exception = assertThrows(SearchServiceException.class, () -> searchService.searchJobs(
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

        assertEquals("SEARCH_CIRCUIT_OPEN", exception.getErrorCode());
        verify(restTemplate, times(4)).exchange(
            any(URI.class),
            eq(HttpMethod.GET),
            eq(HttpEntity.EMPTY),
            any(ParameterizedTypeReference.class)
        );
    }

    @Test
    void searchJobs_WhenPageSizeIsBelowMinimum_ShouldRejectBeforeCallingBackends() {
        SearchServiceException exception = assertThrows(SearchServiceException.class, () -> searchService.searchJobs(
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
        verify(restTemplate, never()).exchange(
            any(URI.class),
            eq(HttpMethod.GET),
            eq(HttpEntity.EMPTY),
            any(ParameterizedTypeReference.class)
        );
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
        result.setCreatedAt(LocalDateTime.now().minusDays(daysOld));
        result.setUpdatedAt(LocalDateTime.now().minusDays(daysOld));
        return result;
    }
}
