package com.jobportal.searchservice.service;

import com.jobportal.searchservice.dto.JobSearchFacetsResponse;
import com.jobportal.searchservice.dto.JobSearchResult;
import com.jobportal.searchservice.dto.PagedResponse;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Comparator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class SearchCacheManager {

    private final MeterRegistry meterRegistry;
    private final long cacheTtlSeconds;
    private final int cacheMaxEntries;
    private final Map<String, CacheEntry<PagedResponse<JobSearchResult>>> searchCache = new ConcurrentHashMap<>();
    private final Map<String, CacheEntry<JobSearchFacetsResponse>> facetsCache = new ConcurrentHashMap<>();

    public SearchCacheManager(
            MeterRegistry meterRegistry,
            @Value("${search.cache.ttl-seconds:30}") long cacheTtlSeconds,
            @Value("${search.cache.max-entries:200}") int cacheMaxEntries) {
        this.meterRegistry = meterRegistry;
        this.cacheTtlSeconds = cacheTtlSeconds;
        this.cacheMaxEntries = cacheMaxEntries;
    }

    public PagedResponse<JobSearchResult> getSearch(String key) {
        return getCached(searchCache, key, "search");
    }

    public void putSearch(String key, PagedResponse<JobSearchResult> value) {
        putCached(searchCache, key, value);
    }

    public JobSearchFacetsResponse getFacets(String key) {
        return getCached(facetsCache, key, "facets");
    }

    public void putFacets(String key, JobSearchFacetsResponse value) {
        putCached(facetsCache, key, value);
    }

    public void clearAll() {
        searchCache.clear();
        facetsCache.clear();
    }

    private <T> T getCached(Map<String, CacheEntry<T>> cache, String key, String cacheName) {
        evictExpiredEntries(cache);
        CacheEntry<T> entry = cache.get(key);
        if (entry == null || entry.isExpired()) {
            if (entry != null) {
                cache.remove(key);
            }
            return null;
        }

        meterRegistry.counter("search.cache.hits", "cache", cacheName).increment();
        return entry.value();
    }

    private <T> void putCached(Map<String, CacheEntry<T>> cache, String key, T value) {
        evictExpiredEntries(cache);
        trimCacheIfNeeded(cache);
        cache.put(key, new CacheEntry<>(value, Instant.now().plusSeconds(cacheTtlSeconds)));
    }

    private <T> void evictExpiredEntries(Map<String, CacheEntry<T>> cache) {
        cache.entrySet().removeIf(entry -> entry.getValue().isExpired());
    }

    private <T> void trimCacheIfNeeded(Map<String, CacheEntry<T>> cache) {
        if (cache.size() < cacheMaxEntries) {
            return;
        }

        String oldestKey = cache.entrySet().stream()
            .min(Map.Entry.comparingByValue(Comparator.comparing(CacheEntry::expiresAt)))
            .map(Map.Entry::getKey)
            .orElse(null);
        if (oldestKey != null) {
            cache.remove(oldestKey);
        }
    }

    private record CacheEntry<T>(T value, Instant expiresAt) {
        private boolean isExpired() {
            return Instant.now().isAfter(expiresAt);
        }
    }
}
