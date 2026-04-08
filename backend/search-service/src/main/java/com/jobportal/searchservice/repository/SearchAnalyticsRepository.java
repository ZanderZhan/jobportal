package com.jobportal.searchservice.repository;

import com.jobportal.searchservice.service.SearchRequest;

public interface SearchAnalyticsRepository {

    void recordZeroResult(String userId, String sessionId, SearchRequest request);

    void recordClick(String userId, String sessionId, Long jobId);

    void recordAbandon(String userId, String sessionId);
}
