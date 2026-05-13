package com.jobportal.profileservice.service;

import com.jobportal.profileservice.dto.ProfileCompletenessResponse;
import com.jobportal.profileservice.entity.StudentProfile;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class ProfileCompletionServiceImpl implements ProfileCompletionService {

    private static final int TOTAL_FIELDS = 9;
    private static final Map<String, String> FIELD_LABELS = Map.of(
            "headline", "Headline",
            "bio", "Bio",
            "location", "Location",
            "phone", "Phone",
            "jobSearchStatus", "Job search status",
            "skills", "Skills",
            "education", "Education",
            "experience", "Experience",
            "portfolioLinks", "Portfolio links"
    );

    @Override
    public ProfileCompletenessResponse calculate(StudentProfile profile) {
        int completedFields = 0;
        List<String> missingFields = new ArrayList<>();

        completedFields += trackScalar(profile.getHeadline(), "headline", missingFields);
        completedFields += trackScalar(profile.getBio(), "bio", missingFields);
        completedFields += trackScalar(profile.getLocation(), "location", missingFields);
        completedFields += trackScalar(profile.getPhone(), "phone", missingFields);
        completedFields += trackScalar(profile.getJobSearchStatus(), "jobSearchStatus", missingFields);
        completedFields += trackCollection(profile.getSkills(), "skills", missingFields);
        completedFields += trackCollection(profile.getEducationEntries(), "education", missingFields);
        completedFields += trackCollection(profile.getExperienceEntries(), "experience", missingFields);
        completedFields += trackCollection(profile.getPortfolioLinks(), "portfolioLinks", missingFields);

        int percentage = (int) Math.round((completedFields * 100.0) / TOTAL_FIELDS);

        return new ProfileCompletenessResponse(
                completedFields,
                TOTAL_FIELDS,
                percentage,
                completedFields == TOTAL_FIELDS,
                List.copyOf(missingFields),
                missingFields.stream()
                        .map(field -> FIELD_LABELS.getOrDefault(field, field))
                        .toList()
        );
    }

    private int trackScalar(String value, String fieldName, List<String> missingFields) {
        if (hasText(value)) {
            return 1;
        }
        missingFields.add(fieldName);
        return 0;
    }

    private int trackCollection(List<?> values, String fieldName, List<String> missingFields) {
        if (values != null && !values.isEmpty()) {
            return 1;
        }
        missingFields.add(fieldName);
        return 0;
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
