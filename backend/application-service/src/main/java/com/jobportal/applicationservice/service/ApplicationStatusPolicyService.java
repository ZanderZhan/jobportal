package com.jobportal.applicationservice.service;

import com.jobportal.applicationservice.entity.ApplicationStatus;

public interface ApplicationStatusPolicyService {

    boolean canWithdraw(ApplicationStatus currentStatus);
}
