package com.jobportal.searchservice.service;

import com.jobportal.searchservice.dto.JobSearchResult;
import com.jobportal.searchservice.dto.PagedResponse;

import java.math.BigDecimal;

public interface SearchService {

    PagedResponse<JobSearchResult> searchJobs(
        String title,
        String company,
        String location,
        String employmentType,
        BigDecimal salaryMin,
        BigDecimal salaryMax,
        String status,
        int page,
        int size,
        String sort
    );
}
