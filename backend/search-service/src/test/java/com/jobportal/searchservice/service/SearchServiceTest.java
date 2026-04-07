package com.jobportal.searchservice.service;

import com.jobportal.searchservice.dto.JobSearchResult;
import com.jobportal.searchservice.dto.PagedResponse;
import com.jobportal.searchservice.exception.SearchServiceException;
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
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SearchServiceTest {

    @Mock
    private RestTemplate restTemplate;

    private SearchServiceImpl searchService;

    @BeforeEach
    void setUp() {
        searchService = new SearchServiceImpl(restTemplate, "http://job-service:8081");
    }

    @Test
    void searchJobs_ShouldProxyToJobService() {
        JobSearchResult result = new JobSearchResult();
        result.setId(1L);
        result.setTitle("Software Engineer");

        PagedResponse<JobSearchResult> response = new PagedResponse<>();
        response.setContent(List.of(result));
        response.setTotalElements(1);

        when(restTemplate.exchange(
                any(URI.class),
                eq(HttpMethod.GET),
                eq(HttpEntity.EMPTY),
                any(ParameterizedTypeReference.class)))
            .thenReturn(ResponseEntity.ok(response));

        PagedResponse<JobSearchResult> actual = searchService.searchJobs(
            "Software",
            null,
            "Limerick",
            "FULL_TIME",
            new BigDecimal("50000"),
            new BigDecimal("70000"),
            null,
            0,
            20,
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
        assertEquals(1, actual.getTotalElements());
        assertTrue(uri.contains("/api/jobs/search"));
        assertTrue(uri.contains("title=Software"));
        assertTrue(uri.contains("location=Limerick"));
        assertTrue(uri.contains("employmentType=FULL_TIME"));
        assertTrue(uri.contains("status=ACTIVE"));
    }

    @Test
    void searchJobs_WhenUpstreamFails_ShouldThrowException() {
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
    }
}
