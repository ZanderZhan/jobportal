package com.jobportal.searchservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.jobportal.searchservice.dto.JobSearchResult;
import com.jobportal.searchservice.dto.SearchIndexStatusResponse;
import com.jobportal.searchservice.exception.SearchServiceException;
import com.jobportal.searchservice.service.SearchIndexAdminService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SearchIndexController.class)
class SearchIndexControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SearchIndexAdminService searchIndexAdminService;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    @Test
    void getStatus_ShouldReturnIndexStatus() throws Exception {
        SearchIndexStatusResponse response = new SearchIndexStatusResponse();
        response.setReady(true);
        response.setReindexInProgress(false);
        response.setDocumentCount(6);
        response.setLastReindexedAt(Instant.now());

        when(searchIndexAdminService.getStatus()).thenReturn(response);

        mockMvc.perform(get("/internal/search/index/status"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.ready").value(true))
            .andExpect(jsonPath("$.documentCount").value(6));
    }

    @Test
    void reindex_ShouldReturnUpdatedStatus() throws Exception {
        SearchIndexStatusResponse response = new SearchIndexStatusResponse();
        response.setReady(true);
        response.setReindexInProgress(false);
        response.setDocumentCount(6);

        when(searchIndexAdminService.reindexJobs()).thenReturn(response);

        mockMvc.perform(post("/internal/search/index/reindex"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.ready").value(true))
            .andExpect(jsonPath("$.documentCount").value(6));
    }

    @Test
    void upsertJob_ShouldReturnNoContent() throws Exception {
        JobSearchResult job = new JobSearchResult();
        job.setId(1L);
        job.setTitle("Software Engineer");

        doNothing().when(searchIndexAdminService).upsertJob(eq(1L), any(JobSearchResult.class));

        mockMvc.perform(put("/internal/search/index/jobs/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(job)))
            .andExpect(status().isNoContent());
    }

    @Test
    void deleteJob_ShouldReturnNoContent() throws Exception {
        doNothing().when(searchIndexAdminService).deleteJob(1L);

        mockMvc.perform(delete("/internal/search/index/jobs/1"))
            .andExpect(status().isNoContent());
    }

    @Test
    void getStatus_WhenIndexIsUnavailable_ShouldReturnServiceUnavailable() throws Exception {
        when(searchIndexAdminService.getStatus()).thenThrow(new SearchServiceException(
            "SEARCH_INDEX_UNAVAILABLE",
            "Search index is unavailable",
            503
        ));

        mockMvc.perform(get("/internal/search/index/status"))
            .andExpect(status().isServiceUnavailable())
            .andExpect(jsonPath("$.code").value("SEARCH_INDEX_UNAVAILABLE"));
    }
}
