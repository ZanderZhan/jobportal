package com.jobportal.searchservice.repository;

import com.jobportal.searchservice.service.SearchRequest;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcSearchAnalyticsRepository implements SearchAnalyticsRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public JdbcSearchAnalyticsRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void recordZeroResult(String userId, String sessionId, SearchRequest request) {
        jdbcTemplate.update("""
            INSERT INTO search_analytics_events (
                user_id, session_id, event_type, title, company, location, employment_type, result_count
            ) VALUES (
                :userId, :sessionId, 'ZERO_RESULT', :title, :company, :location, :employmentType, 0
            )
            """,
            baseParams(userId, sessionId, request)
        );
    }

    @Override
    public void recordClick(String userId, String sessionId, Long jobId) {
        jdbcTemplate.update("""
            INSERT INTO search_analytics_events (
                user_id, session_id, event_type, clicked_job_id
            ) VALUES (
                :userId, :sessionId, 'CLICK', :jobId
            )
            """,
            new MapSqlParameterSource()
                .addValue("userId", userId)
                .addValue("sessionId", sessionId)
                .addValue("jobId", jobId)
        );
    }

    @Override
    public void recordAbandon(String userId, String sessionId) {
        jdbcTemplate.update("""
            INSERT INTO search_analytics_events (
                user_id, session_id, event_type
            ) VALUES (
                :userId, :sessionId, 'ABANDON'
            )
            """,
            new MapSqlParameterSource()
                .addValue("userId", userId)
                .addValue("sessionId", sessionId)
        );
    }

    private MapSqlParameterSource baseParams(String userId, String sessionId, SearchRequest request) {
        return new MapSqlParameterSource()
            .addValue("userId", userId)
            .addValue("sessionId", sessionId)
            .addValue("title", request.title())
            .addValue("company", request.company())
            .addValue("location", request.location())
            .addValue("employmentType", request.primaryEmploymentType());
    }
}
