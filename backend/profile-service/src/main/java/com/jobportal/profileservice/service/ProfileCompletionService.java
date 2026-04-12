package com.jobportal.profileservice.service;

import com.jobportal.profileservice.dto.ProfileCompletenessResponse;
import com.jobportal.profileservice.entity.StudentProfile;

public interface ProfileCompletionService {

    ProfileCompletenessResponse calculate(StudentProfile profile);
}
