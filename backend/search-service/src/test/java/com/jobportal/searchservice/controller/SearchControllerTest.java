package com.jobportal.searchservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.jobportal.searchservice.dto.AutocompleteSuggestion;
import com.jobportal.searchservice.dto.JobAutocompleteResponse;
import com.jobportal.searchservice.dto.JobSearchFacetsResponse;
import com.jobportal.searchservice.dto.JobSearchResult;
import com.jobportal.searchservice.dto.PagedResponse;
import com.jobportal.searchservice.dto.SavedSearchRequest;
import com.jobportal.searchservice.dto.SavedSearchResponse;
import com.jobportal.searchservice.dto.SearchDiscoveryResponse;
import com.jobportal.searchservice.exception.SearchServiceException;
import com.jobportal.searchservice.service.SearchIndexAdminService;
import com.jobportal.searchservice.service.SearchService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SearchController.class)
class SearchControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private ObjectMapper objectMapper;

    @MockitoBean
    private SearchService searchService;

    @MockitoBean
    private SearchIndexAdminService searchIndexAdminService;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    @Test
    void searchJobs_ShouldReturnPagedResults() throws Exception {
        JobSearchResult result = new JobSearchResult();
        result.setId(1L);
        result.setTitle("Software Engineer");
        result.setCompany("Tech Corp");
        result.setLocation("Limerick");
        result.setEmploymentType("FULL_TIME");
        result.setSalaryMin(new BigDecimal("50000"));
        result.setSalaryMax(new BigDecimal("70000"));
        result.setStatus("ACTIVE");
        result.setCreatedAt(LocalDateTime.now());
        result.setUpdatedAt(LocalDateTime.now());

        PagedResponse<JobSearchResult> response = new PagedResponse<>();
        response.setContent(List.of(result));
        response.setTotalElements(1);
        response.setTotalPages(1);
        response.setSize(20);
        response.setNumber(0);
        response.setFirst(true);
        response.setLast(true);

        when(searchService.searchJobs(any(), any(), any(), any(), any(), any(), any(), any(), any(), anyInt(), anyInt(), any()))
            .thenReturn(response);

        mockMvc.perform(get("/api/search/jobs")
                .header("X-Search-User-Id", "user-1")
                .header("X-Search-Session-Id", "session-1")
                .param("title", "Software")
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[0].id").value(1))
            .andExpect(jsonPath("$.content[0].title").value("Software Engineer"))
            .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void searchJobs_ShouldApplyDefaultPublicContractWhenParametersAreOmitted() throws Exception {
        PagedResponse<JobSearchResult> response = new PagedResponse<>();
        response.setContent(List.of());
        response.setTotalElements(0);
        response.setTotalPages(0);
        response.setSize(20);
        response.setNumber(0);
        response.setFirst(true);
        response.setLast(true);

        when(searchService.searchJobs(any(), any(), any(), any(), any(), any(), any(), any(), any(), anyInt(), anyInt(), any()))
            .thenReturn(response);

        mockMvc.perform(get("/api/search/jobs").accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.size").value(20))
            .andExpect(jsonPath("$.number").value(0));

        verify(searchService).searchJobs(
            isNull(),
            isNull(),
            isNull(),
            isNull(),
            isNull(),
            isNull(),
            isNull(),
            isNull(),
            isNull(),
            eq(0),
            eq(20),
            eq("createdAt,desc")
        );
    }

    @Test
    void searchJobs_WhenSearchServiceFails_ShouldReturnMappedError() throws Exception {
        when(searchService.searchJobs(any(), any(), any(), any(), any(), any(), any(), any(), any(), anyInt(), anyInt(), any()))
            .thenThrow(new SearchServiceException(
                "SEARCH_UPSTREAM_FAILED",
                "Failed to fetch search results from job-service",
                502
            ));

        mockMvc.perform(get("/api/search/jobs").accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isBadGateway())
            .andExpect(jsonPath("$.code").value("SEARCH_UPSTREAM_FAILED"))
            .andExpect(jsonPath("$.message").value("Failed to fetch search results from job-service"))
            .andExpect(jsonPath("$.timestamp").isNotEmpty());
    }

    @Test
    void getJobSearchFacets_ShouldReturnFacetBuckets() throws Exception {
        JobSearchFacetsResponse response = new JobSearchFacetsResponse();
        response.setLocations(List.of(new com.jobportal.searchservice.dto.FacetValueCount("Dublin", 2)));
        response.setCompanies(List.of(new com.jobportal.searchservice.dto.FacetValueCount("Northwind", 2)));
        response.setEmploymentTypes(List.of(new com.jobportal.searchservice.dto.FacetValueCount("FULL_TIME", 2)));

        when(searchService.getJobSearchFacets(any(), any(), any(), any(), any(), any(), any()))
            .thenReturn(response);

        mockMvc.perform(get("/api/search/jobs/facets").accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.locations[0].value").value("Dublin"))
            .andExpect(jsonPath("$.companies[0].value").value("Northwind"))
            .andExpect(jsonPath("$.employmentTypes[0].value").value("FULL_TIME"));
    }

    @Test
    void getJobAutocomplete_ShouldReturnSuggestions() throws Exception {
        JobAutocompleteResponse response = new JobAutocompleteResponse();
        response.setSuggestions(List.of(new AutocompleteSuggestion("Software Engineer", "TITLE", 2)));

        when(searchService.getJobAutocomplete("software")).thenReturn(response);

        mockMvc.perform(get("/api/search/jobs/autocomplete")
                .param("query", "software"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.suggestions[0].value").value("Software Engineer"))
            .andExpect(jsonPath("$.suggestions[0].type").value("TITLE"));
    }

    @Test
    void getSearchDiscovery_ShouldReturnRelatedSearchesAndFilters() throws Exception {
        SearchDiscoveryResponse response = new SearchDiscoveryResponse();
        response.setRelatedSearches(List.of("Backend Engineer"));
        response.setSuggestedLocations(List.of(new com.jobportal.searchservice.dto.FacetValueCount("Remote", 2)));

        when(searchService.getSearchDiscovery(any(), any(), any(), any(), any(), any(), any()))
            .thenReturn(response);

        mockMvc.perform(get("/api/search/jobs/discovery"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.relatedSearches[0]").value("Backend Engineer"))
            .andExpect(jsonPath("$.suggestedLocations[0].value").value("Remote"));
    }

    @Test
    void saveSearch_ShouldCreateSavedSearch() throws Exception {
        SavedSearchResponse response = new SavedSearchResponse();
        response.setId(1L);
        response.setName("Remote Engineering");
        response.setTitle("Engineer");
        response.setLocation("Remote");
        response.setCreatedAt(Instant.now());
        response.setUpdatedAt(Instant.now());

        SavedSearchRequest request = new SavedSearchRequest();
        request.setTitle("Engineer");
        request.setLocation("Remote");

        when(searchService.saveSearch(eq("user-1"), any(SavedSearchRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/search/saved-searches")
                .header("X-Search-User-Id", "user-1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.name").value("Remote Engineering"));
    }

    @Test
    void deleteSavedSearch_ShouldReturnNoContent() throws Exception {
        mockMvc.perform(delete("/api/search/saved-searches/1")
                .header("X-Search-User-Id", "user-1"))
            .andExpect(status().isNoContent());

        verify(searchService).deleteSavedSearch("user-1", 1L);
    }

    @Test
    void trackSearchClick_ShouldReturnAccepted() throws Exception {
        mockMvc.perform(post("/api/search/analytics/click")
                .header("X-Search-User-Id", "user-1")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"sessionId":"session-1","jobId":7}
                    """))
            .andExpect(status().isAccepted());
    }
}
