package com.jobportal.profileservice.dto;

import com.jobportal.profileservice.entity.ProfileVisibility;
import com.jobportal.profileservice.entity.StudentProfile;

import java.time.LocalDateTime;
import java.util.List;

public record StudentProfileResponse(
        Long id,
        String userId,
        String headline,
        String bio,
        String location,
        String phone,
        String resumeReference,
        ProfileVisibility visibility,
        String jobSearchStatus,
        List<String> skills,
        List<EducationEntryResponse> education,
        List<ExperienceEntryResponse> experience,
        List<PortfolioLinkResponse> portfolioLinks,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static StudentProfileResponse fromEntity(StudentProfile profile) {
        return new StudentProfileResponse(
                profile.getId(),
                profile.getUserId(),
                profile.getHeadline(),
                profile.getBio(),
                profile.getLocation(),
                profile.getPhone(),
                profile.getResumeReference(),
                profile.getVisibility(),
                profile.getJobSearchStatus(),
                profile.getSkills().stream()
                        .map(skill -> skill.getName())
                        .toList(),
                profile.getEducationEntries().stream()
                        .map(EducationEntryResponse::fromEntity)
                        .toList(),
                profile.getExperienceEntries().stream()
                        .map(ExperienceEntryResponse::fromEntity)
                        .toList(),
                profile.getPortfolioLinks().stream()
                        .map(PortfolioLinkResponse::fromEntity)
                        .toList(),
                profile.getCreatedAt(),
                profile.getUpdatedAt()
        );
    }
}
