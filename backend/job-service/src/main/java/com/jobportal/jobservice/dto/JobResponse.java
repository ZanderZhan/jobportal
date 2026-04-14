package com.jobportal.jobservice.dto;

import com.jobportal.jobservice.entity.Job;
import com.jobportal.jobservice.entity.Job.EmploymentType;
import com.jobportal.jobservice.entity.Job.JobStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record JobResponse(
        Long id,
        String employerId,
        String title,
        String description,
        String company,
        String location,
        EmploymentType employmentType,
        BigDecimal salaryMin,
        BigDecimal salaryMax,
        String salaryCurrency,
        List<String> requirements,
        JobStatus status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static JobResponse fromEntity(Job job) {
        return new JobResponse(
                job.getId(),
                job.getEmployerId(),
                job.getTitle(),
                job.getDescription(),
                job.getCompany(),
                job.getLocation(),
                job.getEmploymentType(),
                job.getSalaryMin(),
                job.getSalaryMax(),
                job.getSalaryCurrency(),
                job.getRequirements(),
                job.getStatus(),
                job.getCreatedAt(),
                job.getUpdatedAt()
        );
    }
}
