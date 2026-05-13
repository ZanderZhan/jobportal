package com.jobportal.profileservice.service;

import com.jobportal.profileservice.dto.ProfileCompletenessResponse;
import com.jobportal.profileservice.dto.StudentProfileResponse;
import com.jobportal.profileservice.dto.StudentProfileUpdateRequest;
import com.jobportal.profileservice.entity.ProfileVisibility;
import com.jobportal.profileservice.entity.StudentProfile;
import com.jobportal.profileservice.exception.ProfileServiceException;
import com.jobportal.profileservice.repository.StudentProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProfileServiceTest {

    @Mock
    private StudentProfileRepository studentProfileRepository;

    private ProfileServiceImpl profileService;

    @BeforeEach
    void setUp() {
        ProfileBootstrapService profileBootstrapService = new ProfileBootstrapServiceImpl(studentProfileRepository);
        ProfileCompletionService profileCompletionService = new ProfileCompletionServiceImpl();
        profileService = new ProfileServiceImpl(
                studentProfileRepository,
                profileBootstrapService,
                profileCompletionService
        );
    }

    @Test
    void getCurrentProfile_ShouldBootstrapWhenMissing() {
        when(studentProfileRepository.findByUserId("student-1")).thenReturn(Optional.empty());
        when(studentProfileRepository.save(any(StudentProfile.class))).thenAnswer(invocation -> {
            StudentProfile profile = invocation.getArgument(0);
            profile.setId(1L);
            return profile;
        });

        StudentProfileResponse response = profileService.getCurrentProfile("student-1", "STUDENT");

        assertEquals("student-1", response.userId());
        assertEquals(ProfileVisibility.PRIVATE, response.visibility());
        verify(studentProfileRepository).save(any(StudentProfile.class));
    }

    @Test
    void getCurrentProfile_ShouldAcceptLegacyJobSeekerRole() {
        StudentProfile profile = StudentProfile.createEmpty("student-1");
        profile.setId(1L);
        when(studentProfileRepository.findByUserId("student-1")).thenReturn(Optional.of(profile));

        StudentProfileResponse response = profileService.getCurrentProfile("student-1", "JOB_SEEKER");

        assertEquals("student-1", response.userId());
    }

    @Test
    void updateCurrentProfile_ShouldPersistStudentProfileSections() {
        StudentProfile profile = StudentProfile.createEmpty("student-1");
        profile.setId(1L);
        when(studentProfileRepository.findByUserId("student-1")).thenReturn(Optional.of(profile));
        when(studentProfileRepository.saveAndFlush(any(StudentProfile.class))).thenAnswer(invocation -> invocation.getArgument(0));

        StudentProfileResponse response = profileService.updateCurrentProfile(
                "student-1",
                "STUDENT",
                new StudentProfileUpdateRequest(
                        "Backend Engineer",
                        "Build APIs",
                        "Limerick",
                        "+353 555 0101",
                        ProfileVisibility.PUBLIC,
                        "OPEN_TO_WORK",
                        List.of("Java", "Spring Boot", " java ", "JAVA"),
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
                )
        );

        assertEquals("Backend Engineer", response.headline());
        assertEquals(ProfileVisibility.PUBLIC, response.visibility());
        assertEquals(2, response.skills().size());
        assertEquals(List.of("Java", "Spring Boot"), response.skills());
        assertEquals(1, response.education().size());
        assertEquals(1, response.experience().size());
        assertEquals(1, response.portfolioLinks().size());
        verify(studentProfileRepository).saveAndFlush(any(StudentProfile.class));
    }

    @Test
    void getCurrentProfileCompleteness_ShouldReturnMissingFieldsForEmptyProfile() {
        StudentProfile profile = StudentProfile.createEmpty("student-1");
        when(studentProfileRepository.findByUserId("student-1")).thenReturn(Optional.of(profile));

        ProfileCompletenessResponse response = profileService.getCurrentProfileCompleteness("student-1", "STUDENT");

        assertEquals(0, response.completedFields());
        assertEquals(9, response.totalFields());
        assertEquals(0, response.percentage());
        assertEquals(List.of(
                "headline",
                "bio",
                "location",
                "phone",
                "jobSearchStatus",
                "skills",
                "education",
                "experience",
                "portfolioLinks"
        ), response.missingFields());
        assertEquals(List.of(
                "Headline",
                "Bio",
                "Location",
                "Phone",
                "Job search status",
                "Skills",
                "Education",
                "Experience",
                "Portfolio links"
        ), response.missingFieldLabels());
    }

    @Test
    void getCurrentProfile_WhenRoleIsNotStudent_ShouldThrowForbidden() {
        ProfileServiceException ex = assertThrows(
                ProfileServiceException.class,
                () -> profileService.getCurrentProfile("employer-1", "HIRING")
        );

        assertEquals("PROFILE_FORBIDDEN", ex.getErrorCode());
        assertEquals(403, ex.getHttpStatus());
    }
}
