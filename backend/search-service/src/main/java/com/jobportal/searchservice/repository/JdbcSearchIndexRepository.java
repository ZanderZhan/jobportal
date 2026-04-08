package com.jobportal.searchservice.repository;

import com.jobportal.searchservice.dto.FacetValueCount;
import com.jobportal.searchservice.dto.JobSearchFacetsResponse;
import com.jobportal.searchservice.dto.JobSearchResult;
import com.jobportal.searchservice.dto.PagedResponse;
import com.jobportal.searchservice.dto.SearchIndexStatusResponse;
import com.jobportal.searchservice.service.SearchRequest;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;

@Repository
public class JdbcSearchIndexRepository implements SearchIndexRepository {

    private static final String STATE_KEY = "jobs";
    private static final String BASE_FROM = """
        FROM job_search_documents d
        WHERE (:title IS NULL OR LOWER(d.title) LIKE :titleLike)
          AND (:company IS NULL OR LOWER(d.company) LIKE :companyLike)
          AND (:location IS NULL OR LOWER(d.location) LIKE :locationLike)
          AND (:employmentType IS NULL OR d.employment_type = :employmentType)
          AND (:salaryMin IS NULL OR d.salary_min >= :salaryMin)
          AND (:salaryMax IS NULL OR d.salary_max <= :salaryMax)
          AND (:status IS NULL OR d.status = :status)
        """;

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public JdbcSearchIndexRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public SearchIndexStatusResponse getStatus() {
        return jdbcTemplate.queryForObject("""
            SELECT ready, reindex_in_progress, document_count, last_reindexed_at, last_incremental_sync_at
            FROM search_index_state
            WHERE state_key = :stateKey
            """,
            new MapSqlParameterSource("stateKey", STATE_KEY),
            (rs, rowNum) -> mapStatus(rs)
        );
    }

    @Override
    public PagedResponse<JobSearchResult> search(SearchRequest request) {
        MapSqlParameterSource params = baseParams(request)
            .addValue("limit", request.size())
            .addValue("offset", request.offset());

        List<JobSearchResult> content = jdbcTemplate.query(
            selectQuery(request),
            params,
            this::mapJob
        );

        Long totalElements = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) " + BASE_FROM,
            baseParams(request),
            Long.class
        );

        PagedResponse<JobSearchResult> response = new PagedResponse<>();
        response.setContent(content);
        response.setTotalElements(totalElements == null ? 0 : totalElements);
        response.setSize(request.size());
        response.setNumber(request.page());
        response.setTotalPages(response.getSize() == 0
            ? 0
            : (int) Math.ceil((double) response.getTotalElements() / response.getSize()));
        response.setFirst(request.page() == 0);
        response.setLast((long) (request.page() + 1) * request.size() >= response.getTotalElements());
        return response;
    }

    @Override
    public JobSearchFacetsResponse getFacets(SearchRequest request, int maxValues) {
        JobSearchFacetsResponse response = new JobSearchFacetsResponse();
        response.setLocations(facetValues("location", request, maxValues));
        response.setCompanies(facetValues("company", request, maxValues));
        response.setEmploymentTypes(facetValues("employment_type", request, maxValues));
        return response;
    }

    @Override
    public void upsert(JobSearchResult job) {
        jdbcTemplate.update(upsertSql(), documentParams(job));
    }

    @Override
    public void upsertAll(List<JobSearchResult> jobs) {
        if (jobs.isEmpty()) {
            return;
        }

        MapSqlParameterSource[] batch = jobs.stream()
            .map(this::documentParams)
            .toArray(MapSqlParameterSource[]::new);
        jdbcTemplate.batchUpdate(upsertSql(), batch);
    }

    @Override
    public void deleteById(Long id) {
        jdbcTemplate.update(
            "DELETE FROM job_search_documents WHERE id = :id",
            new MapSqlParameterSource("id", id)
        );
    }

    @Override
    public void deleteAll() {
        jdbcTemplate.getJdbcTemplate().execute("TRUNCATE TABLE job_search_documents");
        updateDocumentCount();
    }

    @Override
    public void markReindexStarted() {
        jdbcTemplate.update("""
            UPDATE search_index_state
            SET ready = FALSE,
                reindex_in_progress = TRUE
            WHERE state_key = :stateKey
            """,
            new MapSqlParameterSource("stateKey", STATE_KEY)
        );
    }

    @Override
    public void markReindexCompleted(long documentCount) {
        jdbcTemplate.update("""
            UPDATE search_index_state
            SET ready = TRUE,
                reindex_in_progress = FALSE,
                document_count = :documentCount,
                last_reindexed_at = CURRENT_TIMESTAMP
            WHERE state_key = :stateKey
            """,
            new MapSqlParameterSource()
                .addValue("stateKey", STATE_KEY)
                .addValue("documentCount", documentCount)
        );
    }

    @Override
    public void markReindexFailed() {
        jdbcTemplate.update("""
            UPDATE search_index_state
            SET ready = FALSE,
                reindex_in_progress = FALSE
            WHERE state_key = :stateKey
            """,
            new MapSqlParameterSource("stateKey", STATE_KEY)
        );
    }

    @Override
    public void markIncrementalSync() {
        jdbcTemplate.update("""
            UPDATE search_index_state
            SET document_count = :documentCount,
                last_incremental_sync_at = CURRENT_TIMESTAMP
            WHERE state_key = :stateKey
            """,
            new MapSqlParameterSource()
                .addValue("stateKey", STATE_KEY)
                .addValue("documentCount", countDocuments())
        );
    }

    @Override
    public long countDocuments() {
        Long count = jdbcTemplate.getJdbcTemplate().queryForObject(
            "SELECT COUNT(*) FROM job_search_documents",
            Long.class
        );
        return count == null ? 0 : count;
    }

    private String selectQuery(SearchRequest request) {
        return """
            SELECT d.id, d.title, d.description, d.company, d.location, d.employment_type, d.salary_min,
                   d.salary_max, d.salary_currency, d.requirements_text, d.status, d.created_at, d.updated_at
            """ + BASE_FROM + orderByClause(request) + """
            LIMIT :limit
            OFFSET :offset
            """;
    }

    private String orderByClause(SearchRequest request) {
        if (request.usesDefaultSort()) {
            return """
                ORDER BY (
                    (CASE
                        WHEN :title IS NULL THEN 0
                        WHEN LOWER(COALESCE(d.title, '')) = :titleExact THEN 120
                        WHEN LOWER(COALESCE(d.title, '')) LIKE :titleLike THEN 80
                        ELSE 0
                    END)
                    + (CASE
                        WHEN :company IS NULL THEN 0
                        WHEN LOWER(COALESCE(d.company, '')) = :companyExact THEN 90
                        WHEN LOWER(COALESCE(d.company, '')) LIKE :companyLike THEN 60
                        ELSE 0
                    END)
                    + (CASE
                        WHEN :location IS NULL THEN 0
                        WHEN LOWER(COALESCE(d.location, '')) = :locationExact THEN 70
                        WHEN LOWER(COALESCE(d.location, '')) LIKE :locationLike THEN 40
                        ELSE 0
                    END)
                    + (CASE
                        WHEN :title IS NOT NULL AND LOWER(COALESCE(d.description, '')) LIKE :titleLike THEN 25
                        ELSE 0
                    END)
                    + (CASE
                        WHEN d.status = 'ACTIVE' THEN 25
                        ELSE 0
                    END)
                    + (CASE
                        WHEN d.created_at >= CURRENT_TIMESTAMP - INTERVAL '1 day' THEN 35
                        WHEN d.created_at >= CURRENT_TIMESTAMP - INTERVAL '7 days' THEN 25
                        WHEN d.created_at >= CURRENT_TIMESTAMP - INTERVAL '30 days' THEN 15
                        ELSE 5
                    END)
                ) DESC,
                d.created_at DESC,
                d.id ASC
                """;
        }

        String[] sortParts = request.sort().split(",");
        String sortField = switch (sortParts[0]) {
            case "title" -> "d.title";
            case "company" -> "d.company";
            case "location" -> "d.location";
            case "employmentType" -> "d.employment_type";
            case "salaryMin" -> "d.salary_min";
            case "salaryMax" -> "d.salary_max";
            case "status" -> "d.status";
            case "updatedAt" -> "d.updated_at";
            default -> "d.created_at";
        };

        return " ORDER BY " + sortField + " " + sortParts[1] + ", d.id ASC ";
    }

    private List<FacetValueCount> facetValues(String field, SearchRequest request, int maxValues) {
        String alias = switch (field) {
            case "employment_type" -> "employment_type";
            case "company" -> "company";
            default -> "location";
        };

        MapSqlParameterSource params = baseParams(request).addValue("limit", maxValues);
        return jdbcTemplate.query("""
            SELECT d.%s AS value, COUNT(*) AS count
            """.formatted(alias) + BASE_FROM + """
              AND d.%s IS NOT NULL
              AND d.%s <> ''
            GROUP BY d.%s
            ORDER BY COUNT(*) DESC, d.%s ASC
            LIMIT :limit
            """.formatted(alias, alias, alias, alias),
            params,
            (rs, rowNum) -> new FacetValueCount(rs.getString("value"), rs.getLong("count"))
        );
    }

    private MapSqlParameterSource baseParams(SearchRequest request) {
        return new MapSqlParameterSource()
            .addValue("title", request.title())
            .addValue("titleExact", lower(request.title()))
            .addValue("titleLike", likePattern(request.title()))
            .addValue("company", request.company())
            .addValue("companyExact", lower(request.company()))
            .addValue("companyLike", likePattern(request.company()))
            .addValue("location", request.location())
            .addValue("locationExact", lower(request.location()))
            .addValue("locationLike", likePattern(request.location()))
            .addValue("employmentType", request.employmentType())
            .addValue("salaryMin", request.salaryMin())
            .addValue("salaryMax", request.salaryMax())
            .addValue("status", request.status());
    }

    private MapSqlParameterSource documentParams(JobSearchResult job) {
        return new MapSqlParameterSource()
            .addValue("id", job.getId())
            .addValue("title", job.getTitle())
            .addValue("description", job.getDescription())
            .addValue("company", job.getCompany())
            .addValue("location", job.getLocation())
            .addValue("employmentType", job.getEmploymentType())
            .addValue("salaryMin", job.getSalaryMin())
            .addValue("salaryMax", job.getSalaryMax())
            .addValue("salaryCurrency", job.getSalaryCurrency())
            .addValue("requirementsText", joinRequirements(job.getRequirements()))
            .addValue("searchText", buildSearchText(job))
            .addValue("status", job.getStatus())
            .addValue("createdAt", job.getCreatedAt())
            .addValue("updatedAt", job.getUpdatedAt());
    }

    private String upsertSql() {
        return """
            INSERT INTO job_search_documents (
                id, title, description, company, location, employment_type, salary_min, salary_max,
                salary_currency, requirements_text, search_text, status, created_at, updated_at
            ) VALUES (
                :id, :title, :description, :company, :location, :employmentType, :salaryMin, :salaryMax,
                :salaryCurrency, :requirementsText, :searchText, :status, :createdAt, :updatedAt
            )
            ON CONFLICT (id) DO UPDATE SET
                title = EXCLUDED.title,
                description = EXCLUDED.description,
                company = EXCLUDED.company,
                location = EXCLUDED.location,
                employment_type = EXCLUDED.employment_type,
                salary_min = EXCLUDED.salary_min,
                salary_max = EXCLUDED.salary_max,
                salary_currency = EXCLUDED.salary_currency,
                requirements_text = EXCLUDED.requirements_text,
                search_text = EXCLUDED.search_text,
                status = EXCLUDED.status,
                created_at = EXCLUDED.created_at,
                updated_at = EXCLUDED.updated_at
            """;
    }

    private JobSearchResult mapJob(ResultSet rs, int rowNum) throws SQLException {
        JobSearchResult result = new JobSearchResult();
        result.setId(rs.getLong("id"));
        result.setTitle(rs.getString("title"));
        result.setDescription(rs.getString("description"));
        result.setCompany(rs.getString("company"));
        result.setLocation(rs.getString("location"));
        result.setEmploymentType(rs.getString("employment_type"));
        result.setSalaryMin(rs.getBigDecimal("salary_min"));
        result.setSalaryMax(rs.getBigDecimal("salary_max"));
        result.setSalaryCurrency(rs.getString("salary_currency"));
        result.setRequirements(splitRequirements(rs.getString("requirements_text")));
        result.setStatus(rs.getString("status"));
        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            result.setCreatedAt(createdAt.toLocalDateTime());
        }
        Timestamp updatedAt = rs.getTimestamp("updated_at");
        if (updatedAt != null) {
            result.setUpdatedAt(updatedAt.toLocalDateTime());
        }
        return result;
    }

    private SearchIndexStatusResponse mapStatus(ResultSet rs) throws SQLException {
        SearchIndexStatusResponse response = new SearchIndexStatusResponse();
        response.setReady(rs.getBoolean("ready"));
        response.setReindexInProgress(rs.getBoolean("reindex_in_progress"));
        response.setDocumentCount(rs.getLong("document_count"));
        Timestamp lastReindexedAt = rs.getTimestamp("last_reindexed_at");
        if (lastReindexedAt != null) {
            response.setLastReindexedAt(lastReindexedAt.toInstant());
        }
        Timestamp lastIncrementalSyncAt = rs.getTimestamp("last_incremental_sync_at");
        if (lastIncrementalSyncAt != null) {
            response.setLastIncrementalSyncAt(lastIncrementalSyncAt.toInstant());
        }
        return response;
    }

    private String buildSearchText(JobSearchResult job) {
        return String.join(" ",
            safe(job.getTitle()),
            safe(job.getDescription()),
            safe(job.getCompany()),
            safe(job.getLocation()),
            safe(job.getEmploymentType()),
            safe(job.getStatus()),
            safe(joinRequirements(job.getRequirements()))
        ).trim();
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private String lower(String value) {
        return value == null ? null : value.toLowerCase();
    }

    private String likePattern(String value) {
        return value == null ? null : "%" + value.toLowerCase() + "%";
    }

    private String joinRequirements(List<String> requirements) {
        if (requirements == null || requirements.isEmpty()) {
            return null;
        }
        return String.join("\n", requirements);
    }

    private List<String> splitRequirements(String requirementsText) {
        if (requirementsText == null || requirementsText.isBlank()) {
            return List.of();
        }
        return Arrays.asList(requirementsText.split("\\n"));
    }

    private void updateDocumentCount() {
        jdbcTemplate.update("""
            UPDATE search_index_state
            SET document_count = :documentCount
            WHERE state_key = :stateKey
            """,
            new MapSqlParameterSource()
                .addValue("stateKey", STATE_KEY)
                .addValue("documentCount", countDocuments())
        );
    }
}
