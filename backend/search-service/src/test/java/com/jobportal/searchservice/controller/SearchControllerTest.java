package com.jobportal.searchservice.controller;

import com.jobportal.searchservice.dto.JobSearchResult;
import com.jobportal.searchservice.dto.PagedResponse;
import com.jobportal.searchservice.service.SearchService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SearchController.class)
class SearchControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SearchService searchService;

    @Test
    void searchJobs_ShouldReturnPagedResults() throws Exception {
        JobSearchResult result = new JobSearchResult();
        result.setId(1L);
        result.setTitle("Software Engineer");
        result.setCompany("Tech Corp");
        result.setLocation("Limerick");
        result.setEmploymentType("FULL_TIME");
        result.setSalaryMin(new BigDecimal("50000"));
        result.setSalaryMax(new BigDecimal("70000"));
        result.setStatus("ACTIVE");
        result.setCreatedAt(LocalDateTime.now());
        result.setUpdatedAt(LocalDateTime.now());

        PagedResponse<JobSearchResult> response = new PagedResponse<>();
        response.setContent(List.of(result));
        response.setTotalElements(1);
        response.setTotalPages(1);
        response.setSize(20);
        response.setNumber(0);
        response.setFirst(true);
        response.setLast(true);

        when(searchService.searchJobs(any(), any(), any(), any(), any(), any(), any(), anyInt(), anyInt(), any()))
            .thenReturn(response);

        mockMvc.perform(get("/api/search/jobs")
                .param("title", "Software")
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[0].id").value(1))
            .andExpect(jsonPath("$.content[0].title").value("Software Engineer"))
            .andExpect(jsonPath("$.totalElements").value(1));
    }
}
