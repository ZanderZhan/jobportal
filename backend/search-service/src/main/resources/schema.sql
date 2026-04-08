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
