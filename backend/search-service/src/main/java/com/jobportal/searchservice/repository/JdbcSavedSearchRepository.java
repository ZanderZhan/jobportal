package com.jobportal.searchservice.repository;

import com.jobportal.searchservice.dto.SavedSearchResponse;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

@Repository
public class JdbcSavedSearchRepository implements SavedSearchRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public JdbcSavedSearchRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<SavedSearchResponse> findByUserId(String userId) {
        return jdbcTemplate.query("""
            SELECT id, name, title, company, location, employment_type, created_at, updated_at
            FROM saved_searches
            WHERE user_id = :userId
            ORDER BY updated_at DESC, id DESC
            """,
            new MapSqlParameterSource("userId", userId),
            (rs, rowNum) -> mapSavedSearch(rs.getLong("id"),
                rs.getString("name"),
                rs.getString("title"),
                rs.getString("company"),
                rs.getString("location"),
                rs.getString("employment_type"),
                rs.getTimestamp("created_at"),
                rs.getTimestamp("updated_at"))
        );
    }

    @Override
    public SavedSearchResponse save(String userId, SavedSearchResponse savedSearch) {
        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update("""
            INSERT INTO saved_searches (
                user_id, name, title, company, location, employment_type, created_at, updated_at
            ) VALUES (
                :userId, :name, :title, :company, :location, :employmentType, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
            )
            """,
            new MapSqlParameterSource()
                .addValue("userId", userId)
                .addValue("name", savedSearch.getName())
                .addValue("title", savedSearch.getTitle())
                .addValue("company", savedSearch.getCompany())
                .addValue("location", savedSearch.getLocation())
                .addValue("employmentType", savedSearch.getEmploymentType()),
            keyHolder,
            new String[]{"id"}
        );

        Number key = keyHolder.getKey();
        if (key == null) {
            throw new IllegalStateException("Failed to create saved search id");
        }

        return findByIdAndUserId(key.longValue(), userId)
            .orElseThrow(() -> new IllegalStateException("Saved search was created but could not be loaded"));
    }

    @Override
    public Optional<SavedSearchResponse> findByIdAndUserId(Long id, String userId) {
        List<SavedSearchResponse> results = jdbcTemplate.query("""
            SELECT id, name, title, company, location, employment_type, created_at, updated_at
            FROM saved_searches
            WHERE id = :id AND user_id = :userId
            """,
            new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("userId", userId),
            (rs, rowNum) -> mapSavedSearch(rs.getLong("id"),
                rs.getString("name"),
                rs.getString("title"),
                rs.getString("company"),
                rs.getString("location"),
                rs.getString("employment_type"),
                rs.getTimestamp("created_at"),
                rs.getTimestamp("updated_at"))
        );
        return results.stream().findFirst();
    }

    @Override
    public void deleteById(Long id) {
        jdbcTemplate.update(
            "DELETE FROM saved_searches WHERE id = :id",
            new MapSqlParameterSource("id", id)
        );
    }

    private SavedSearchResponse mapSavedSearch(
            Long id,
            String name,
            String title,
            String company,
            String location,
            String employmentType,
            Timestamp createdAt,
            Timestamp updatedAt) {
        SavedSearchResponse response = new SavedSearchResponse();
        response.setId(id);
        response.setName(name);
        response.setTitle(title);
        response.setCompany(company);
        response.setLocation(location);
        response.setEmploymentType(employmentType);
        if (createdAt != null) {
            response.setCreatedAt(createdAt.toInstant());
        }
        if (updatedAt != null) {
            response.setUpdatedAt(updatedAt.toInstant());
        }
        return response;
    }
}
