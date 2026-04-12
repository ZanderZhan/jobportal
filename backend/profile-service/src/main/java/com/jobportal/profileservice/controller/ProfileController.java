package com.jobportal.profileservice.controller;

import com.jobportal.profileservice.dto.ProfileCompletenessResponse;
import com.jobportal.profileservice.dto.StudentProfileResponse;
import com.jobportal.profileservice.dto.StudentProfileUpdateRequest;
import com.jobportal.profileservice.service.ProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/profiles")
@Tag(name = "Profiles", description = "Profile management APIs")
public class ProfileController {

    private final ProfileService profileService;

    public ProfileController(ProfileService profileService) {
        this.profileService = profileService;
    }

    @GetMapping("/me")
    @Operation(summary = "Get the authenticated student's profile")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Profile retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Missing user context"),
            @ApiResponse(responseCode = "403", description = "Only students can access this operation")
    })
    public ResponseEntity<StudentProfileResponse> getCurrentProfile(
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @RequestHeader(value = "X-User-Role", required = false) String userRole) {
        return ResponseEntity.ok(profileService.getCurrentProfile(userId, userRole));
    }

    @PutMapping("/me")
    @Operation(summary = "Update the authenticated student's profile")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Profile updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input"),
            @ApiResponse(responseCode = "401", description = "Missing user context"),
            @ApiResponse(responseCode = "403", description = "Only students can access this operation")
    })
    public ResponseEntity<StudentProfileResponse> updateCurrentProfile(
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @RequestHeader(value = "X-User-Role", required = false) String userRole,
            @Valid @RequestBody StudentProfileUpdateRequest request) {
        return ResponseEntity.ok(profileService.updateCurrentProfile(userId, userRole, request));
    }

    @GetMapping("/me/completeness")
    @Operation(summary = "Get the authenticated student's profile completeness summary")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Profile completeness retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Missing user context"),
            @ApiResponse(responseCode = "403", description = "Only students can access this operation")
    })
    public ResponseEntity<ProfileCompletenessResponse> getCurrentProfileCompleteness(
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @RequestHeader(value = "X-User-Role", required = false) String userRole) {
        return ResponseEntity.ok(profileService.getCurrentProfileCompleteness(userId, userRole));
    }
}
