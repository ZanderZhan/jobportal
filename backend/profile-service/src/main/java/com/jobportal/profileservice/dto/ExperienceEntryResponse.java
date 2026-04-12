package com.jobportal.profileservice.dto;

import com.jobportal.profileservice.entity.StudentProfileExperience;

import java.time.LocalDate;

public record ExperienceEntryResponse(
        Long id,
        String company,
        String title,
        String description,
        LocalDate startDate,
        LocalDate endDate
) {

    public static ExperienceEntryResponse fromEntity(StudentProfileExperience experience) {
        return new ExperienceEntryResponse(
                experience.getId(),
                experience.getCompany(),
                experience.getTitle(),
                experience.getDescription(),
                experience.getStartDate(),
                experience.getEndDate()
        );
    }
}
