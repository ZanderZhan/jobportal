package com.jobportal.searchservice.service;

import com.jobportal.searchservice.dto.JobAutocompleteResponse;
import com.jobportal.searchservice.dto.JobSearchResult;
import com.jobportal.searchservice.dto.JobSearchFacetsResponse;
import com.jobportal.searchservice.dto.PagedResponse;
import com.jobportal.searchservice.dto.SavedSearchRequest;
import com.jobportal.searchservice.dto.SavedSearchResponse;
import com.jobportal.searchservice.dto.SearchAbandonRequest;
import com.jobportal.searchservice.dto.SearchClickRequest;
import com.jobportal.searchservice.dto.SearchDiscoveryResponse;

import java.math.BigDecimal;
import java.util.List;

public interface SearchService {

    PagedResponse<JobSearchResult> searchJobs(
        String userId,
        String sessionId,
        String title,
        String company,
        String location,
        String employmentType,
        String employmentTypes,
        BigDecimal salaryMin,
        BigDecimal salaryMax,
        String salaryCurrency,
        String workMode,
        Integer postedWithinDays,
        String status,
        int page,
        int size,
        String sort
    );

    JobSearchFacetsResponse getJobSearchFacets(
        String title,
        String company,
        String location,
        String employmentType,
        String employmentTypes,
        BigDecimal salaryMin,
        BigDecimal salaryMax,
        String salaryCurrency,
        String workMode,
        Integer postedWithinDays,
        String status
    );

    JobAutocompleteResponse getJobAutocomplete(String query);

    SearchDiscoveryResponse getSearchDiscovery(
        String title,
        String company,
        String location,
        String employmentType,
        String employmentTypes,
        BigDecimal salaryMin,
        BigDecimal salaryMax,
        String salaryCurrency,
        String workMode,
        Integer postedWithinDays,
        String status
    );

    List<SavedSearchResponse> getSavedSearches(String userId);

    SavedSearchResponse saveSearch(String userId, SavedSearchRequest request);

    void deleteSavedSearch(String userId, Long id);

    void trackSearchClick(String userId, SearchClickRequest request);

    void trackSearchAbandon(String userId, SearchAbandonRequest request);
}
