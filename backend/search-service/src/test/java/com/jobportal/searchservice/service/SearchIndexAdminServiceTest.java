package com.jobportal.searchservice.service;

import com.jobportal.searchservice.dto.JobSearchResult;
import com.jobportal.searchservice.dto.PagedResponse;
import com.jobportal.searchservice.dto.SearchIndexStatusResponse;
import com.jobportal.searchservice.exception.SearchServiceException;
import com.jobportal.searchservice.repository.SearchIndexRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.net.URI;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SearchIndexAdminServiceTest {

    @Mock
    private SearchIndexRepository searchIndexRepository;

    @Mock
    private SearchCacheManager searchCacheManager;

    @Mock
    private RestTemplate restTemplate;

    private SearchIndexAdminServiceImpl adminService;

    @BeforeEach
    void setUp() {
        adminService = new SearchIndexAdminServiceImpl(
            searchIndexRepository,
            searchCacheManager,
            restTemplate,
            "http://job-service:8081",
            100
        );
    }

    @Test
    void reindexJobs_ShouldBackfillIndexAndReturnStatus() {
        JobSearchResult job = createJob(1L, "Software Engineer");
        PagedResponse<JobSearchResult> page = new PagedResponse<>();
        page.setContent(List.of(job));
        page.setLast(true);

        SearchIndexStatusResponse status = new SearchIndexStatusResponse();
        status.setReady(true);
        status.setDocumentCount(1);

        when(restTemplate.exchange(
                any(URI.class),
                eq(HttpMethod.GET),
                eq(HttpEntity.EMPTY),
                any(ParameterizedTypeReference.class)))
            .thenReturn(ResponseEntity.ok(page));
        when(searchIndexRepository.getStatus()).thenReturn(status);

        SearchIndexStatusResponse actual = adminService.reindexJobs();

        assertEquals(1, actual.getDocumentCount());
        verify(searchIndexRepository).markReindexStarted();
        verify(searchIndexRepository).deleteAll();
        verify(searchIndexRepository).upsertAll(List.of(job));
        verify(searchIndexRepository).markReindexCompleted(1);
        verify(searchCacheManager, times(2)).clearAll();
    }

    @Test
    void upsertJob_WhenPathIdDoesNotMatchPayload_ShouldRejectRequest() {
        JobSearchResult job = createJob(2L, "Software Engineer");

        SearchServiceException exception = assertThrows(SearchServiceException.class, () -> adminService.upsertJob(1L, job));

        assertEquals("SEARCH_INVALID_REQUEST", exception.getErrorCode());
    }

    @Test
    void deleteJob_ShouldInvalidateCachesAndUpdateIndex() {
        adminService.deleteJob(1L);

        verify(searchIndexRepository).deleteById(1L);
        verify(searchIndexRepository).markIncrementalSync();
        verify(searchCacheManager).clearAll();
    }

    private JobSearchResult createJob(Long id, String title) {
        JobSearchResult result = new JobSearchResult();
        result.setId(id);
        result.setTitle(title);
        result.setDescription("Build search features");
        result.setCompany("Northwind");
        result.setLocation("Dublin");
        result.setEmploymentType("FULL_TIME");
        result.setStatus("ACTIVE");
        result.setSalaryMin(new BigDecimal("60000"));
        result.setSalaryMax(new BigDecimal("80000"));
        result.setCreatedAt(LocalDateTime.now());
        result.setUpdatedAt(LocalDateTime.now());
        return result;
    }
}
