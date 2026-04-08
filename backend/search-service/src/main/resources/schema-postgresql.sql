CREATE TABLE IF NOT EXISTS job_search_documents (
    id BIGINT PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    description TEXT NOT NULL,
    company VARCHAR(255) NOT NULL,
    location VARCHAR(255),
    employment_type VARCHAR(64),
    salary_min NUMERIC(12, 2),
    salary_max NUMERIC(12, 2),
    salary_currency VARCHAR(3),
    requirements_text TEXT,
    search_text TEXT NOT NULL,
    status VARCHAR(64) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS search_index_state (
    state_key VARCHAR(32) PRIMARY KEY,
    ready BOOLEAN NOT NULL DEFAULT FALSE,
    reindex_in_progress BOOLEAN NOT NULL DEFAULT FALSE,
    document_count BIGINT NOT NULL DEFAULT 0,
    last_reindexed_at TIMESTAMP NULL,
    last_incremental_sync_at TIMESTAMP NULL
);

INSERT INTO search_index_state (state_key, ready, reindex_in_progress, document_count)
VALUES ('jobs', FALSE, FALSE, 0)
ON CONFLICT (state_key) DO NOTHING;

CREATE INDEX IF NOT EXISTS idx_job_search_documents_status
    ON job_search_documents (status);

CREATE INDEX IF NOT EXISTS idx_job_search_documents_employment_type
    ON job_search_documents (employment_type);

CREATE INDEX IF NOT EXISTS idx_job_search_documents_created_at
    ON job_search_documents (created_at DESC);

CREATE INDEX IF NOT EXISTS idx_job_search_documents_updated_at
    ON job_search_documents (updated_at DESC);

CREATE INDEX IF NOT EXISTS idx_job_search_documents_title_lower
    ON job_search_documents (LOWER(title));

CREATE INDEX IF NOT EXISTS idx_job_search_documents_company_lower
    ON job_search_documents (LOWER(company));

CREATE INDEX IF NOT EXISTS idx_job_search_documents_location_lower
    ON job_search_documents (LOWER(location));

CREATE INDEX IF NOT EXISTS idx_job_search_documents_fts
    ON job_search_documents
    USING GIN (to_tsvector('simple', search_text));

CREATE TABLE IF NOT EXISTS saved_searches (
    id BIGSERIAL PRIMARY KEY,
    user_id VARCHAR(255) NOT NULL,
    name VARCHAR(255) NOT NULL,
    title VARCHAR(255),
    company VARCHAR(255),
    location VARCHAR(255),
    employment_type VARCHAR(64),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_saved_searches_user_id
    ON saved_searches (user_id, updated_at DESC);

CREATE TABLE IF NOT EXISTS search_analytics_events (
    id BIGSERIAL PRIMARY KEY,
    user_id VARCHAR(255),
    session_id VARCHAR(255),
    event_type VARCHAR(64) NOT NULL,
    title VARCHAR(255),
    company VARCHAR(255),
    location VARCHAR(255),
    employment_type VARCHAR(64),
    result_count INTEGER,
    clicked_job_id BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_search_analytics_user_id
    ON search_analytics_events (user_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_search_analytics_session_id
    ON search_analytics_events (session_id, created_at DESC);
