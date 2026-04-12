package com.jobportal.searchservice.controller;

import com.jobportal.searchservice.dto.JobSearchResult;
import com.jobportal.searchservice.dto.SearchIndexStatusResponse;
import com.jobportal.searchservice.service.SearchIndexAdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/search/index")
@Tag(name = "Search Index", description = "Internal search index management APIs")
public class SearchIndexController {

    private final SearchIndexAdminService searchIndexAdminService;

    public SearchIndexController(SearchIndexAdminService searchIndexAdminService) {
        this.searchIndexAdminService = searchIndexAdminService;
    }

    @GetMapping("/status")
    @Operation(summary = "Get search index status")
    public ResponseEntity<SearchIndexStatusResponse> getStatus() {
        return ResponseEntity.ok(searchIndexAdminService.getStatus());
    }

    @PostMapping("/reindex")
    @Operation(summary = "Rebuild the search index from job-service")
    public ResponseEntity<SearchIndexStatusResponse> reindex() {
        return ResponseEntity.ok(searchIndexAdminService.reindexJobs());
    }

    @PutMapping("/jobs/{id}")
    @Operation(summary = "Upsert a job document into the search index")
    public ResponseEntity<Void> upsertJob(@PathVariable Long id, @RequestBody JobSearchResult job) {
        searchIndexAdminService.upsertJob(id, job);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/jobs/{id}")
    @Operation(summary = "Delete a job document from the search index")
    public ResponseEntity<Void> deleteJob(@PathVariable Long id) {
        searchIndexAdminService.deleteJob(id);
        return ResponseEntity.noContent().build();
    }
}
