package com.jobportal.profileservice.service;

import com.jobportal.profileservice.entity.StudentProfile;

public interface ProfileBootstrapService {

    StudentProfile getOrCreateStudentProfile(String userId);
}
