package com.jobportal.applicationservice.controller;

import com.jobportal.applicationservice.dto.ApplicationCreateRequest;
import com.jobportal.applicationservice.dto.ApplicationResponse;
import com.jobportal.applicationservice.dto.ApplicationStatusUpdateRequest;
import com.jobportal.applicationservice.service.ApplicationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/applications")
@Tag(name = "Applications", description = "Application management APIs")
public class ApplicationController {

    private final ApplicationService applicationService;

    public ApplicationController(ApplicationService applicationService) {
        this.applicationService = applicationService;
    }

    @PostMapping
    @Operation(summary = "Submit a new application")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Application submitted successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input or job is not eligible"),
            @ApiResponse(responseCode = "401", description = "Missing user context"),
            @ApiResponse(responseCode = "403", description = "Only students can submit applications"),
            @ApiResponse(responseCode = "409", description = "Duplicate application")
    })
    public ResponseEntity<ApplicationResponse> submitApplication(
            @RequestHeader(value = "X-User-Id", required = false) String studentId,
            @RequestHeader(value = "X-User-Role", required = false) String userRole,
            @Valid @RequestBody ApplicationCreateRequest request,
            UriComponentsBuilder uriComponentsBuilder) {
        ApplicationResponse response = applicationService.submitApplication(studentId, userRole, request);
        URI location = uriComponentsBuilder.path("/api/applications/{id}")
                .buildAndExpand(response.id())
                .toUri();
        return ResponseEntity.status(HttpStatus.CREATED).location(location).body(response);
    }

    @GetMapping
    @Operation(summary = "List applications for the authenticated student")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Applications retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Missing user context"),
            @ApiResponse(responseCode = "403", description = "Only students can access applications")
    })
    public ResponseEntity<List<ApplicationResponse>> getStudentApplications(
            @RequestHeader(value = "X-User-Id", required = false) String studentId,
            @RequestHeader(value = "X-User-Role", required = false) String userRole) {
        return ResponseEntity.ok(applicationService.getStudentApplications(studentId, userRole));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get application details for the authenticated student")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Application retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Missing user context"),
            @ApiResponse(responseCode = "403", description = "Only students can access applications"),
            @ApiResponse(responseCode = "404", description = "Application not found")
    })
    public ResponseEntity<ApplicationResponse> getStudentApplicationById(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Id", required = false) String studentId,
            @RequestHeader(value = "X-User-Role", required = false) String userRole) {
        return ResponseEntity.ok(applicationService.getStudentApplicationById(id, studentId, userRole));
    }

    @PutMapping("/{id}/withdraw")
    @Operation(summary = "Withdraw an application for the authenticated student")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Application withdrawn successfully"),
            @ApiResponse(responseCode = "401", description = "Missing user context"),
            @ApiResponse(responseCode = "403", description = "Only students can withdraw applications"),
            @ApiResponse(responseCode = "404", description = "Application not found"),
            @ApiResponse(responseCode = "409", description = "Withdrawal is not allowed for the current status")
    })
    public ResponseEntity<ApplicationResponse> withdrawApplication(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Id", required = false) String studentId,
            @RequestHeader(value = "X-User-Role", required = false) String userRole) {
        return ResponseEntity.ok(applicationService.withdrawApplication(id, studentId, userRole));
    }

    @GetMapping("/jobs/{jobId}")
    @Operation(summary = "List applications for a job owned by the authenticated employer")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Applications retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Missing user context"),
            @ApiResponse(responseCode = "403", description = "Only employers can access this operation"),
            @ApiResponse(responseCode = "404", description = "Job not found")
    })
    public ResponseEntity<List<ApplicationResponse>> getEmployerApplicationsForJob(
            @PathVariable Long jobId,
            @RequestHeader(value = "X-User-Id", required = false) String employerId,
            @RequestHeader(value = "X-User-Role", required = false) String userRole) {
        return ResponseEntity.ok(applicationService.getEmployerApplicationsForJob(jobId, employerId, userRole));
    }

    @PutMapping("/{id}/status")
    @Operation(summary = "Update an application status as the authenticated employer")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Application status updated successfully"),
            @ApiResponse(responseCode = "401", description = "Missing user context"),
            @ApiResponse(responseCode = "403", description = "Only employers can access this operation"),
            @ApiResponse(responseCode = "404", description = "Application or job not found"),
            @ApiResponse(responseCode = "409", description = "Status transition is not allowed")
    })
    public ResponseEntity<ApplicationResponse> updateApplicationStatus(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Id", required = false) String employerId,
            @RequestHeader(value = "X-User-Role", required = false) String userRole,
            @Valid @RequestBody ApplicationStatusUpdateRequest request) {
        return ResponseEntity.ok(applicationService.updateApplicationStatus(id, employerId, userRole, request));
    }
}
