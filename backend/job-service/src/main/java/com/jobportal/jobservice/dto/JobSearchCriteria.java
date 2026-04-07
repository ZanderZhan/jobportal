package com.jobportal.jobservice.dto;

import com.jobportal.jobservice.entity.Job.EmploymentType;
import com.jobportal.jobservice.entity.Job.JobStatus;

import java.math.BigDecimal;

public record JobSearchCriteria(
        String title,
        String company,
        String location,
        EmploymentType employmentType,
        BigDecimal salaryMin,
        BigDecimal salaryMax,
        JobStatus status,
        String employerId
) {}
