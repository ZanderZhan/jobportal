package com.jobportal.profileservice;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobportal.profileservice.dto.StudentProfileUpdateRequest;
import com.jobportal.profileservice.entity.ProfileVisibility;
import com.jobportal.profileservice.repository.StudentProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ProfileWorkflowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private StudentProfileRepository studentProfileRepository;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        studentProfileRepository.deleteAll();
        objectMapper = new ObjectMapper().findAndRegisterModules();
    }

    @Test
    void getCurrentProfile_ShouldBootstrapEmptyStudentProfile() throws Exception {
        mockMvc.perform(get("/api/profiles/me")
                        .header("X-User-Id", "student-77")
                        .header("X-User-Role", "STUDENT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value("student-77"))
                .andExpect(jsonPath("$.visibility").value("PRIVATE"))
                .andExpect(jsonPath("$.skills").isArray());

        assertEquals(1, studentProfileRepository.count());
        assertEquals("student-77", studentProfileRepository.findAll().getFirst().getUserId());
    }

    @Test
    void updateCurrentProfile_ShouldPersistProfileSectionsAndCompleteness() throws Exception {
        mockMvc.perform(put("/api/profiles/me")
                        .header("X-User-Id", "student-88")
                        .header("X-User-Role", "STUDENT")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new StudentProfileUpdateRequest(
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
                                        "https://github.com/student-88"
                                ))
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.headline").value("Backend Engineer"))
                .andExpect(jsonPath("$.visibility").value("PUBLIC"))
                .andExpect(jsonPath("$.skills[1]").value("Spring Boot"))
                .andExpect(jsonPath("$.portfolioLinks[0].url").value("https://github.com/student-88"));

        mockMvc.perform(get("/api/profiles/me/completeness")
                        .header("X-User-Id", "student-88")
                        .header("X-User-Role", "STUDENT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.completedFields").value(9))
                .andExpect(jsonPath("$.percentage").value(100))
                .andExpect(jsonPath("$.complete").value(true))
                .andExpect(jsonPath("$.missingFields").isEmpty());

        var savedProfile = studentProfileRepository.findByUserId("student-88").orElseThrow();
        assertEquals("Backend Engineer", savedProfile.getHeadline());
        assertEquals(2, savedProfile.getSkills().size());
        assertEquals(1, savedProfile.getEducationEntries().size());
        assertEquals(1, savedProfile.getExperienceEntries().size());
        assertEquals(1, savedProfile.getPortfolioLinks().size());
    }
}
