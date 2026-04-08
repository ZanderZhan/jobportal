package com.jobportal.searchservice.service;

import com.jobportal.searchservice.dto.JobSearchFacetsResponse;
import com.jobportal.searchservice.dto.JobSearchResult;
import com.jobportal.searchservice.dto.PagedResponse;
import com.jobportal.searchservice.exception.SearchServiceException;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.ParameterizedTypeReference;
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
    private RestTemplate restTemplate;

    private SimpleMeterRegistry meterRegistry;
    private SearchServiceImpl searchService;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        searchService = new SearchServiceImpl(
            restTemplate,
            meterRegistry,
            "http://job-service:8081",
            100,
            200,
            30,
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
    void searchJobs_ShouldNormalizeQueryRankResultsAndApplyCaching() {
        JobSearchResult partialMatch = createJob(2L, "Engineer", "Acme", "Limerick", "FULL_TIME", "ACTIVE", 5);
        JobSearchResult exactMatch = createJob(1L, "Software Engineer", "Northwind", "Dublin", "FULL_TIME", "ACTIVE", 10);

        PagedResponse<JobSearchResult> response = new PagedResponse<>();
        response.setContent(List.of(partialMatch, exactMatch));
        response.setTotalElements(2);

        when(restTemplate.exchange(
                any(URI.class),
                eq(HttpMethod.GET),
                eq(HttpEntity.EMPTY),
                any(ParameterizedTypeReference.class)))
            .thenReturn(ResponseEntity.ok(response));

        PagedResponse<JobSearchResult> actual = searchService.searchJobs(
            "  Software   Engineer  ",
            "   ",
            "  Limerick  ",
            " full-time ",
            new BigDecimal("50000"),
            new BigDecimal("70000"),
            " active ",
            0,
            250,
            " createdAt, DESC "
        );
        PagedResponse<JobSearchResult> cached = searchService.searchJobs(
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
        verify(restTemplate, times(1)).exchange(
            uriCaptor.capture(),
            eq(HttpMethod.GET),
            eq(HttpEntity.EMPTY),
            any(ParameterizedTypeReference.class)
        );

        String uri = uriCaptor.getValue().toString();
        assertEquals(2, actual.getTotalElements());
        assertTrue(uri.contains("/api/jobs/search"));
        assertTrue(uri.contains("title=Software%20Engineer"));
        assertTrue(uri.contains("location=Limerick"));
        assertTrue(uri.contains("employmentType=FULL_TIME"));
        assertTrue(uri.contains("salaryMin=50000"));
        assertTrue(uri.contains("salaryMax=70000"));
        assertTrue(uri.contains("status=ACTIVE"));
        assertTrue(uri.contains("page=0"));
        assertTrue(uri.contains("size=200"));
        assertTrue(uri.contains("sort=createdAt,desc"));
        assertTrue(!uri.contains("company="));
        assertEquals("Software Engineer", actual.getContent().get(0).getTitle());
        assertEquals(2, cached.getTotalElements());
        assertEquals(1.0, meterRegistry.get("search.requests").tag("outcome", "success").counter().count());
        assertEquals(1.0, meterRegistry.get("search.requests").tag("outcome", "cache_hit").counter().count());
    }

    @Test
    void searchJobs_WhenStatusProvided_ShouldForwardRequestedStatus() {
        PagedResponse<JobSearchResult> response = new PagedResponse<>();
        response.setContent(List.of());
        response.setTotalElements(0);

        when(restTemplate.exchange(
                any(URI.class),
                eq(HttpMethod.GET),
                eq(HttpEntity.EMPTY),
                any(ParameterizedTypeReference.class)))
            .thenReturn(ResponseEntity.ok(response));

        searchService.searchJobs(
            null,
            null,
            null,
            null,
            null,
            null,
            "closed",
            1,
            10,
            "title,asc"
        );

        ArgumentCaptor<URI> uriCaptor = ArgumentCaptor.forClass(URI.class);
        verify(restTemplate).exchange(
            uriCaptor.capture(),
            eq(HttpMethod.GET),
            eq(HttpEntity.EMPTY),
            any(ParameterizedTypeReference.class)
        );

        String uri = uriCaptor.getValue().toString();
        assertTrue(uri.contains("status=CLOSED"));
        assertTrue(uri.contains("page=1"));
        assertTrue(uri.contains("size=10"));
        assertTrue(uri.contains("sort=title,asc"));
    }

    @Test
    void getJobSearchFacets_ShouldAggregateTopFacetValues() {
        PagedResponse<JobSearchResult> response = new PagedResponse<>();
        response.setContent(List.of(
            createJob(1L, "Frontend Engineer", "Northwind", "Dublin", "FULL_TIME", "ACTIVE", 2),
            createJob(2L, "Backend Engineer", "Northwind", "Dublin", "FULL_TIME", "ACTIVE", 4),
            createJob(3L, "Data Analyst", "Insight Works", "Remote", "CONTRACT", "ACTIVE", 1)
        ));
        response.setTotalElements(3);

        when(restTemplate.exchange(
                any(URI.class),
                eq(HttpMethod.GET),
                eq(HttpEntity.EMPTY),
                any(ParameterizedTypeReference.class)))
            .thenReturn(ResponseEntity.ok(response));

        JobSearchFacetsResponse facets = searchService.getJobSearchFacets(
            null,
            null,
            null,
            null,
            null,
            null,
            null
        );

        assertEquals("Dublin", facets.getLocations().get(0).getValue());
        assertEquals(2L, facets.getLocations().get(0).getCount());
        assertEquals("Northwind", facets.getCompanies().get(0).getValue());
        assertEquals(2L, facets.getCompanies().get(0).getCount());
        assertEquals("FULL_TIME", facets.getEmploymentTypes().get(0).getValue());
        assertEquals(2L, facets.getEmploymentTypes().get(0).getCount());
    }

    @Test
    void searchJobs_WhenRequestIsInvalid_ShouldRejectBeforeCallingUpstream() {
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
        assertEquals(400, exception.getHttpStatus());
        assertEquals(1.0, meterRegistry.get("search.requests").tag("outcome", "validation_error").counter().count());
        verifyNoInteractions(restTemplate);
    }

    @Test
    void searchJobs_WhenSalaryRangeIsInvalid_ShouldRejectBeforeCallingUpstream() {
        SearchServiceException exception = assertThrows(SearchServiceException.class, () -> searchService.searchJobs(
            null,
            null,
            null,
            null,
            new BigDecimal("90000"),
            new BigDecimal("70000"),
            null,
            0,
            20,
            "createdAt,desc"
        ));

        assertEquals("SEARCH_INVALID_REQUEST", exception.getErrorCode());
        assertEquals(400, exception.getHttpStatus());
        verifyNoInteractions(restTemplate);
    }

    @Test
    void searchJobs_WhenUpstreamBodyIsEmpty_ShouldThrowException() {
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
        assertEquals(502, exception.getHttpStatus());
        assertEquals(1.0, meterRegistry.get("search.requests").tag("outcome", "upstream_error").counter().count());
        assertEquals(1.0, meterRegistry.get("search.upstream.errors").tag("code", "SEARCH_UPSTREAM_EMPTY").counter().count());
    }

    @Test
    void searchJobs_WhenUpstreamFails_ShouldThrowException() {
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
        assertEquals(502, exception.getHttpStatus());
        assertEquals(1.0, meterRegistry.get("search.requests").tag("outcome", "upstream_error").counter().count());
        assertEquals(1.0, meterRegistry.get("search.upstream.errors").tag("code", "SEARCH_UPSTREAM_FAILED").counter().count());
    }

    @Test
    void searchJobs_WhenCircuitBreakerOpens_ShouldRejectSubsequentRequests() {
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
        assertEquals(503, exception.getHttpStatus());
        verify(restTemplate, times(4)).exchange(
            any(URI.class),
            eq(HttpMethod.GET),
            eq(HttpEntity.EMPTY),
            any(ParameterizedTypeReference.class)
        );
    }

    @Test
    void searchJobs_WhenPageSizeIsBelowMinimum_ShouldRejectBeforeCallingUpstream() {
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
        assertEquals(400, exception.getHttpStatus());
        verify(restTemplate, never()).exchange(
            any(URI.class),
            eq(HttpMethod.GET),
            eq(HttpEntity.EMPTY),
            any(ParameterizedTypeReference.class)
        );
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
