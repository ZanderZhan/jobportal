package com.jobportal.profileservice.service;

import com.jobportal.profileservice.dto.ProfileCompletenessResponse;
import com.jobportal.profileservice.dto.StudentProfileResponse;
import com.jobportal.profileservice.dto.StudentProfileUpdateRequest;

public interface ProfileService {

    StudentProfileResponse getCurrentProfile(String userId, String userRole);

    StudentProfileResponse updateCurrentProfile(String userId, String userRole, StudentProfileUpdateRequest request);

    ProfileCompletenessResponse getCurrentProfileCompleteness(String userId, String userRole);
}
