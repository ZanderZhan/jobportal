package com.jobportal.searchservice.repository;

import com.jobportal.searchservice.dto.SavedSearchResponse;

import java.util.List;
import java.util.Optional;

public interface SavedSearchRepository {

    List<SavedSearchResponse> findByUserId(String userId);

    SavedSearchResponse save(String userId, SavedSearchResponse savedSearch);

    Optional<SavedSearchResponse> findByIdAndUserId(Long id, String userId);

    void deleteById(Long id);
}
