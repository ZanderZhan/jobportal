package com.jobportal.searchservice.dto;

import java.time.Instant;

public class SearchIndexStatusResponse {

    private boolean ready;
    private boolean reindexInProgress;
    private long documentCount;
    private Instant lastReindexedAt;
    private Instant lastIncrementalSyncAt;

    public boolean isReady() {
        return ready;
    }

    public void setReady(boolean ready) {
        this.ready = ready;
    }

    public boolean isReindexInProgress() {
        return reindexInProgress;
    }

    public void setReindexInProgress(boolean reindexInProgress) {
        this.reindexInProgress = reindexInProgress;
    }

    public long getDocumentCount() {
        return documentCount;
    }

    public void setDocumentCount(long documentCount) {
        this.documentCount = documentCount;
    }

    public Instant getLastReindexedAt() {
        return lastReindexedAt;
    }

    public void setLastReindexedAt(Instant lastReindexedAt) {
        this.lastReindexedAt = lastReindexedAt;
    }

    public Instant getLastIncrementalSyncAt() {
        return lastIncrementalSyncAt;
    }

    public void setLastIncrementalSyncAt(Instant lastIncrementalSyncAt) {
        this.lastIncrementalSyncAt = lastIncrementalSyncAt;
    }
}
