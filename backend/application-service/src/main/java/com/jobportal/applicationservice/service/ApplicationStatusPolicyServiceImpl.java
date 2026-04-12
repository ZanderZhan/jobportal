package com.jobportal.applicationservice.service;

import com.jobportal.applicationservice.entity.ApplicationStatus;
import org.springframework.stereotype.Service;

@Service
public class ApplicationStatusPolicyServiceImpl implements ApplicationStatusPolicyService {

    @Override
    public boolean canWithdraw(ApplicationStatus currentStatus) {
        return currentStatus == ApplicationStatus.SUBMITTED
                || currentStatus == ApplicationStatus.UNDER_REVIEW;
    }
}
