package com.jobportal.searchservice.controller;

import com.jobportal.searchservice.dto.JobSearchFacetsResponse;
import com.jobportal.searchservice.dto.JobSearchResult;
import com.jobportal.searchservice.dto.PagedResponse;
import com.jobportal.searchservice.service.SearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/search")
@Tag(name = "Search", description = "Search APIs")
public class SearchController {

    private final SearchService searchService;

    public SearchController(SearchService searchService) {
        this.searchService = searchService;
    }

    @GetMapping("/jobs")
    @Operation(summary = "Search job listings")
    public ResponseEntity<PagedResponse<JobSearchResult>> searchJobs(
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String company,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) String employmentType,
            @RequestParam(required = false) BigDecimal salaryMin,
            @RequestParam(required = false) BigDecimal salaryMax,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt,desc") String sort) {

        PagedResponse<JobSearchResult> response = searchService.searchJobs(
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

        return ResponseEntity.ok(response);
    }

    @GetMapping("/jobs/facets")
    @Operation(summary = "Get job search facets")
    public ResponseEntity<JobSearchFacetsResponse> getJobSearchFacets(
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String company,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) String employmentType,
            @RequestParam(required = false) BigDecimal salaryMin,
            @RequestParam(required = false) BigDecimal salaryMax,
            @RequestParam(required = false) String status) {

        JobSearchFacetsResponse response = searchService.getJobSearchFacets(
            title,
            company,
            location,
            employmentType,
            salaryMin,
            salaryMax,
            status
        );

        return ResponseEntity.ok(response);
    }
}
