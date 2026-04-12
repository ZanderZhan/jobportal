package com.jobportal.profileservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.jobportal.profileservice.dto.EducationEntryResponse;
import com.jobportal.profileservice.dto.ExperienceEntryResponse;
import com.jobportal.profileservice.dto.PortfolioLinkResponse;
import com.jobportal.profileservice.dto.ProfileCompletenessResponse;
import com.jobportal.profileservice.dto.StudentProfileResponse;
import com.jobportal.profileservice.dto.StudentProfileUpdateRequest;
import com.jobportal.profileservice.entity.ProfileVisibility;
import com.jobportal.profileservice.exception.GlobalExceptionHandler;
import com.jobportal.profileservice.service.ProfileService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProfileController.class)
@Import(GlobalExceptionHandler.class)
class ProfileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProfileService profileService;

    private ObjectMapper objectMapper;
    private StudentProfileResponse profileResponse;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        profileResponse = new StudentProfileResponse(
                1L,
                "student-1",
                "Backend Engineer",
                "Build APIs",
                "Limerick",
                "+353 555 0101",
                null,
                ProfileVisibility.PUBLIC,
                "OPEN_TO_WORK",
                List.of("Java", "Spring Boot"),
                List.of(new EducationEntryResponse(
                        11L,
                        "University of Limerick",
                        "BSc",
                        "Computer Science",
                        LocalDate.of(2022, 9, 1),
                        LocalDate.of(2026, 5, 31)
                )),
                List.of(new ExperienceEntryResponse(
                        21L,
                        "Acme",
                        "Intern",
                        "Built internal tools",
                        LocalDate.of(2025, 6, 1),
                        LocalDate.of(2025, 8, 31)
                )),
                List.of(new PortfolioLinkResponse(
                        31L,
                        "GitHub",
                        "https://github.com/student-1"
                )),
                LocalDateTime.of(2026, 4, 12, 18, 0),
                LocalDateTime.of(2026, 4, 12, 19, 0)
        );
    }

    @Test
    void getCurrentProfile_ShouldReturnProfile() throws Exception {
        when(profileService.getCurrentProfile("student-1", "STUDENT"))
                .thenReturn(profileResponse);

        mockMvc.perform(get("/api/profiles/me")
                        .header("X-User-Id", "student-1")
                        .header("X-User-Role", "STUDENT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value("student-1"))
                .andExpect(jsonPath("$.headline").value("Backend Engineer"))
                .andExpect(jsonPath("$.skills[0]").value("Java"))
                .andExpect(jsonPath("$.education[0].institution").value("University of Limerick"));
    }

    @Test
    void updateCurrentProfile_ShouldReturnUpdatedProfile() throws Exception {
        StudentProfileUpdateRequest request = new StudentProfileUpdateRequest(
                "Backend Engineer",
                "Build APIs",
                "Limerick",
                "+353 555 0101",
                ProfileVisibility.PUBLIC,
                "OPEN_TO_WORK",
                List.of("Java", "Spring Boot"),
                List.of(new com.jobportal.profileservice.dto.EducationEntryRequest(
                        "University of Limerick",
                        "BSc",
                        "Computer Science",
                        LocalDate.of(2022, 9, 1),
                        LocalDate.of(2026, 5, 31)
                )),
                List.of(new com.jobportal.profileservice.dto.ExperienceEntryRequest(
                        "Acme",
                        "Intern",
                        "Built internal tools",
                        LocalDate.of(2025, 6, 1),
                        LocalDate.of(2025, 8, 31)
                )),
                List.of(new com.jobportal.profileservice.dto.PortfolioLinkRequest(
                        "GitHub",
                        "https://github.com/student-1"
                ))
        );

        when(profileService.updateCurrentProfile(eq("student-1"), eq("STUDENT"), any(StudentProfileUpdateRequest.class)))
                .thenReturn(profileResponse);

        mockMvc.perform(put("/api/profiles/me")
                        .header("X-User-Id", "student-1")
                        .header("X-User-Role", "STUDENT")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.visibility").value("PUBLIC"))
                .andExpect(jsonPath("$.experience[0].title").value("Intern"));
    }

    @Test
    void updateCurrentProfile_WithInvalidRequest_ShouldReturnBadRequest() throws Exception {
        StudentProfileUpdateRequest invalidRequest = new StudentProfileUpdateRequest(
                null,
                null,
                null,
                "invalid-phone!",
                null,
                null,
                null,
                null,
                null,
                null
        );

        mockMvc.perform(put("/api/profiles/me")
                        .header("X-User-Id", "student-1")
                        .header("X-User-Role", "STUDENT")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors.phone").value("Phone contains invalid characters"));
    }

    @Test
    void getCurrentProfileCompleteness_ShouldReturnSummary() throws Exception {
        when(profileService.getCurrentProfileCompleteness("student-1", "STUDENT"))
                .thenReturn(new ProfileCompletenessResponse(
                        6,
                        9,
                        67,
                        false,
                        List.of("phone", "experience", "portfolioLinks")
                ));

        mockMvc.perform(get("/api/profiles/me/completeness")
                        .header("X-User-Id", "student-1")
                        .header("X-User-Role", "STUDENT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.completedFields").value(6))
                .andExpect(jsonPath("$.percentage").value(67))
                .andExpect(jsonPath("$.missingFields[0]").value("phone"));
    }
}
