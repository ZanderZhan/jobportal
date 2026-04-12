package com.jobportal.profileservice.dto;

import com.jobportal.profileservice.entity.StudentProfileEducation;

import java.time.LocalDate;

public record EducationEntryResponse(
        Long id,
        String institution,
        String degree,
        String fieldOfStudy,
        LocalDate startDate,
        LocalDate endDate
) {

    public static EducationEntryResponse fromEntity(StudentProfileEducation education) {
        return new EducationEntryResponse(
                education.getId(),
                education.getInstitution(),
                education.getDegree(),
                education.getFieldOfStudy(),
                education.getStartDate(),
                education.getEndDate()
        );
    }
}
