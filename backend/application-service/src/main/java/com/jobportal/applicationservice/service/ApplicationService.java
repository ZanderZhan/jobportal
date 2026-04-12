package com.jobportal.applicationservice.service;

import com.jobportal.applicationservice.dto.ApplicationCreateRequest;
import com.jobportal.applicationservice.dto.ApplicationResponse;
import com.jobportal.applicationservice.dto.ApplicationStatusUpdateRequest;

import java.util.List;

public interface ApplicationService {

    ApplicationResponse submitApplication(String studentId, String userRole, ApplicationCreateRequest request);

    List<ApplicationResponse> getStudentApplications(String studentId, String userRole);

    ApplicationResponse getStudentApplicationById(Long applicationId, String studentId, String userRole);

    ApplicationResponse withdrawApplication(Long applicationId, String studentId, String userRole);

    List<ApplicationResponse> getEmployerApplicationsForJob(Long jobId, String employerId, String userRole);

    ApplicationResponse updateApplicationStatus(Long applicationId, String employerId, String userRole, ApplicationStatusUpdateRequest request);
}
