package com.jobportal.applicationservice.service;

import com.jobportal.applicationservice.entity.ApplicationStatus;
import org.springframework.stereotype.Service;

import java.util.EnumSet;
import java.util.Map;

@Service
public class ApplicationStatusPolicyServiceImpl implements ApplicationStatusPolicyService {

    private static final Map<ApplicationStatus, EnumSet<ApplicationStatus>> ALLOWED_TRANSITIONS = Map.of(
            ApplicationStatus.SUBMITTED, EnumSet.of(ApplicationStatus.UNDER_REVIEW, ApplicationStatus.REJECTED),
            ApplicationStatus.UNDER_REVIEW, EnumSet.of(ApplicationStatus.INTERVIEW, ApplicationStatus.HIRED, ApplicationStatus.REJECTED),
            ApplicationStatus.INTERVIEW, EnumSet.of(ApplicationStatus.HIRED, ApplicationStatus.REJECTED),
            ApplicationStatus.HIRED, EnumSet.noneOf(ApplicationStatus.class),
            ApplicationStatus.REJECTED, EnumSet.noneOf(ApplicationStatus.class),
            ApplicationStatus.WITHDRAWN, EnumSet.noneOf(ApplicationStatus.class)
    );

    @Override
    public boolean canWithdraw(ApplicationStatus currentStatus) {
        return currentStatus == ApplicationStatus.SUBMITTED
                || currentStatus == ApplicationStatus.UNDER_REVIEW;
    }

    @Override
    public boolean canTransition(ApplicationStatus currentStatus, ApplicationStatus nextStatus) {
        if (currentStatus == null || nextStatus == null || currentStatus == nextStatus) {
            return false;
        }
        return ALLOWED_TRANSITIONS.getOrDefault(currentStatus, EnumSet.noneOf(ApplicationStatus.class))
                .contains(nextStatus);
    }
}
