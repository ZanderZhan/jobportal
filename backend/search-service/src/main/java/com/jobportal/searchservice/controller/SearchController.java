package com.jobportal.searchservice.controller;

import com.jobportal.searchservice.dto.JobAutocompleteResponse;
import com.jobportal.searchservice.dto.JobSearchFacetsResponse;
import com.jobportal.searchservice.dto.JobSearchResult;
import com.jobportal.searchservice.dto.PagedResponse;
import com.jobportal.searchservice.dto.SavedSearchRequest;
import com.jobportal.searchservice.dto.SavedSearchResponse;
import com.jobportal.searchservice.dto.SearchAbandonRequest;
import com.jobportal.searchservice.dto.SearchClickRequest;
import com.jobportal.searchservice.dto.SearchDiscoveryResponse;
import com.jobportal.searchservice.service.SearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/search")
@Tag(name = "Search", description = "Search APIs")
public class SearchController {

    private static final String USER_ID_HEADER = "X-Search-User-Id";
    private static final String SESSION_ID_HEADER = "X-Search-Session-Id";

    private final SearchService searchService;

    public SearchController(SearchService searchService) {
        this.searchService = searchService;
    }

    @GetMapping("/jobs")
    @Operation(summary = "Search job listings")
    public ResponseEntity<PagedResponse<JobSearchResult>> searchJobs(
            @RequestHeader(name = USER_ID_HEADER, required = false) String userId,
            @RequestHeader(name = SESSION_ID_HEADER, required = false) String sessionId,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String company,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) String employmentType,
            @RequestParam(required = false) String employmentTypes,
            @RequestParam(required = false) BigDecimal salaryMin,
            @RequestParam(required = false) BigDecimal salaryMax,
            @RequestParam(required = false) String salaryCurrency,
            @RequestParam(required = false) String workMode,
            @RequestParam(required = false) Integer postedWithinDays,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt,desc") String sort) {

        PagedResponse<JobSearchResult> response = searchService.searchJobs(
            userId,
            sessionId,
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

        return ResponseEntity.ok(response);
    }

    @GetMapping("/jobs/facets")
    @Operation(summary = "Get job search facets")
    public ResponseEntity<JobSearchFacetsResponse> getJobSearchFacets(
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String company,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) String employmentType,
            @RequestParam(required = false) String employmentTypes,
            @RequestParam(required = false) BigDecimal salaryMin,
            @RequestParam(required = false) BigDecimal salaryMax,
            @RequestParam(required = false) String salaryCurrency,
            @RequestParam(required = false) String workMode,
            @RequestParam(required = false) Integer postedWithinDays,
            @RequestParam(required = false) String status) {

        JobSearchFacetsResponse response = searchService.getJobSearchFacets(
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
            status
        );

        return ResponseEntity.ok(response);
    }

    @GetMapping("/jobs/autocomplete")
    @Operation(summary = "Get job search autocomplete suggestions")
    public ResponseEntity<JobAutocompleteResponse> getJobAutocomplete(
            @RequestParam String query) {
        return ResponseEntity.ok(searchService.getJobAutocomplete(query));
    }

    @GetMapping("/jobs/discovery")
    @Operation(summary = "Get suggested filters and related searches")
    public ResponseEntity<SearchDiscoveryResponse> getSearchDiscovery(
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String company,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) String employmentType,
            @RequestParam(required = false) String employmentTypes,
            @RequestParam(required = false) BigDecimal salaryMin,
            @RequestParam(required = false) BigDecimal salaryMax,
            @RequestParam(required = false) String salaryCurrency,
            @RequestParam(required = false) String workMode,
            @RequestParam(required = false) Integer postedWithinDays,
            @RequestParam(required = false) String status) {
        return ResponseEntity.ok(searchService.getSearchDiscovery(
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
            status
        ));
    }

    @GetMapping("/saved-searches")
    @Operation(summary = "Get saved searches for a user")
    public ResponseEntity<List<SavedSearchResponse>> getSavedSearches(
            @RequestHeader(name = USER_ID_HEADER, required = false) String userId) {
        return ResponseEntity.ok(searchService.getSavedSearches(userId));
    }

    @PostMapping("/saved-searches")
    @Operation(summary = "Save a search for a user")
    public ResponseEntity<SavedSearchResponse> saveSearch(
            @RequestHeader(name = USER_ID_HEADER, required = false) String userId,
            @RequestBody SavedSearchRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(searchService.saveSearch(userId, request));
    }

    @DeleteMapping("/saved-searches/{id}")
    @Operation(summary = "Delete a saved search")
    public ResponseEntity<Void> deleteSavedSearch(
            @RequestHeader(name = USER_ID_HEADER, required = false) String userId,
            @PathVariable Long id) {
        searchService.deleteSavedSearch(userId, id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/analytics/click")
    @Operation(summary = "Track a search result click")
    public ResponseEntity<Void> trackSearchClick(
            @RequestHeader(name = USER_ID_HEADER, required = false) String userId,
            @RequestBody SearchClickRequest request) {
        searchService.trackSearchClick(userId, request);
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/analytics/abandon")
    @Operation(summary = "Track an abandoned search session")
    public ResponseEntity<Void> trackSearchAbandon(
            @RequestHeader(name = USER_ID_HEADER, required = false) String userId,
            @RequestBody SearchAbandonRequest request) {
        searchService.trackSearchAbandon(userId, request);
        return ResponseEntity.accepted().build();
    }
}
