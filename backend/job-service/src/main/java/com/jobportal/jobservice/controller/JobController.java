package com.jobportal.jobservice.controller;

import com.jobportal.jobservice.dto.JobRequest;
import com.jobportal.jobservice.dto.JobResponse;
import com.jobportal.jobservice.dto.JobSearchCriteria;
import com.jobportal.jobservice.entity.Job.EmploymentType;
import com.jobportal.jobservice.entity.Job.JobStatus;
import com.jobportal.jobservice.service.JobService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/jobs")
@Tag(name = "Jobs", description = "Job management APIs")
public class JobController {

    private final JobService jobService;

    public JobController(JobService jobService) {
        this.jobService = jobService;
    }

    @PostMapping
    @Operation(summary = "Create a new job posting")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Job created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input")
    })
    public ResponseEntity<JobResponse> createJob(
            @Valid @RequestBody JobRequest request,
            Authentication authentication) {
        JobResponse response = jobService.createJob(request, resolveCallerId(authentication));
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get job by ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Job found"),
            @ApiResponse(responseCode = "404", description = "Job not found")
    })
    public ResponseEntity<JobResponse> getJobById(
            @Parameter(description = "Job ID") @PathVariable Long id) {
        JobResponse response = jobService.getJobById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    @Operation(summary = "Get all jobs with pagination")
    @ApiResponse(responseCode = "200", description = "Jobs retrieved successfully")
    public ResponseEntity<Page<JobResponse>> getAllJobs(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<JobResponse> jobs = jobService.getAllJobs(pageable);
        return ResponseEntity.ok(jobs);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an existing job posting")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Job updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input"),
            @ApiResponse(responseCode = "403", description = "Forbidden – caller does not own this job"),
            @ApiResponse(responseCode = "404", description = "Job not found")
    })
    public ResponseEntity<JobResponse> updateJob(
            @Parameter(description = "Job ID") @PathVariable Long id,
            @Valid @RequestBody JobRequest request,
            Authentication authentication) {
        JobResponse response = jobService.updateJob(id, request, resolveCallerId(authentication));
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a job posting")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Job deleted successfully"),
            @ApiResponse(responseCode = "403", description = "Forbidden – caller does not own this job"),
            @ApiResponse(responseCode = "404", description = "Job not found")
    })
    public ResponseEntity<Void> deleteJob(
            @Parameter(description = "Job ID") @PathVariable Long id,
            Authentication authentication) {
        jobService.deleteJob(id, resolveCallerId(authentication));
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/search")
    @Operation(summary = "Search jobs with filters")
    @ApiResponse(responseCode = "200", description = "Search results retrieved successfully")
    public ResponseEntity<Page<JobResponse>> searchJobs(
            @Parameter(description = "Job title (partial match)") @RequestParam(required = false) String title,
            @Parameter(description = "Company name (partial match)") @RequestParam(required = false) String company,
            @Parameter(description = "Location (partial match)") @RequestParam(required = false) String location,
            @Parameter(description = "Employment type") @RequestParam(required = false) EmploymentType employmentType,
            @Parameter(description = "Minimum salary") @RequestParam(required = false) BigDecimal salaryMin,
            @Parameter(description = "Maximum salary") @RequestParam(required = false) BigDecimal salaryMax,
            @Parameter(description = "Job status") @RequestParam(required = false) JobStatus status,
            @Parameter(description = "Employer / owner ID") @RequestParam(required = false) String employerId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        JobSearchCriteria criteria = new JobSearchCriteria(title, company, location, employmentType, salaryMin, salaryMax, status, employerId);

        Page<JobResponse> results = jobService.searchJobs(criteria, pageable);
        return ResponseEntity.ok(results);
    }

    private String resolveCallerId(Authentication authentication) {
        return authentication != null ? authentication.getName() : null;
    }
}
