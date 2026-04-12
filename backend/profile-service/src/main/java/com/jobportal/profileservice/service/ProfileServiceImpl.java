package com.jobportal.profileservice.service;

import com.jobportal.profileservice.dto.EducationEntryRequest;
import com.jobportal.profileservice.dto.ExperienceEntryRequest;
import com.jobportal.profileservice.dto.PortfolioLinkRequest;
import com.jobportal.profileservice.dto.ProfileCompletenessResponse;
import com.jobportal.profileservice.dto.StudentProfileResponse;
import com.jobportal.profileservice.dto.StudentProfileUpdateRequest;
import com.jobportal.profileservice.entity.StudentProfile;
import com.jobportal.profileservice.entity.StudentProfileEducation;
import com.jobportal.profileservice.entity.StudentProfileExperience;
import com.jobportal.profileservice.entity.StudentProfilePortfolioLink;
import com.jobportal.profileservice.entity.StudentProfileSkill;
import com.jobportal.profileservice.exception.ProfileServiceException;
import com.jobportal.profileservice.repository.StudentProfileRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
public class ProfileServiceImpl implements ProfileService {

    private static final String STUDENT_ROLE = "STUDENT";
    private static final String JOB_SEEKER_ROLE = "JOB_SEEKER";

    private final StudentProfileRepository studentProfileRepository;
    private final ProfileBootstrapService profileBootstrapService;
    private final ProfileCompletionService profileCompletionService;

    public ProfileServiceImpl(
            StudentProfileRepository studentProfileRepository,
            ProfileBootstrapService profileBootstrapService,
            ProfileCompletionService profileCompletionService) {
        this.studentProfileRepository = studentProfileRepository;
        this.profileBootstrapService = profileBootstrapService;
        this.profileCompletionService = profileCompletionService;
    }

    @Override
    public StudentProfileResponse getCurrentProfile(String userId, String userRole) {
        String normalizedUserId = requireStudentContext(userId, userRole);
        StudentProfile profile = profileBootstrapService.getOrCreateStudentProfile(normalizedUserId);
        return StudentProfileResponse.fromEntity(profile);
    }

    @Override
    public StudentProfileResponse updateCurrentProfile(String userId, String userRole, StudentProfileUpdateRequest request) {
        String normalizedUserId = requireStudentContext(userId, userRole);
        StudentProfile profile = profileBootstrapService.getOrCreateStudentProfile(normalizedUserId);

        applyRequest(profile, request);

        StudentProfile savedProfile = studentProfileRepository.saveAndFlush(profile);
        return StudentProfileResponse.fromEntity(savedProfile);
    }

    @Override
    public ProfileCompletenessResponse getCurrentProfileCompleteness(String userId, String userRole) {
        String normalizedUserId = requireStudentContext(userId, userRole);
        StudentProfile profile = profileBootstrapService.getOrCreateStudentProfile(normalizedUserId);
        return profileCompletionService.calculate(profile);
    }

    private void applyRequest(StudentProfile profile, StudentProfileUpdateRequest request) {
        if (request.headline() != null) {
            profile.setHeadline(normalize(request.headline()));
        }
        if (request.bio() != null) {
            profile.setBio(normalize(request.bio()));
        }
        if (request.location() != null) {
            profile.setLocation(normalize(request.location()));
        }
        if (request.phone() != null) {
            profile.setPhone(normalize(request.phone()));
        }
        if (request.visibility() != null) {
            profile.setVisibility(request.visibility());
        }
        if (request.jobSearchStatus() != null) {
            profile.setJobSearchStatus(normalize(request.jobSearchStatus()));
        }
        if (request.skills() != null) {
            profile.replaceSkills(mapSkills(request.skills()));
        }
        if (request.education() != null) {
            profile.replaceEducationEntries(mapEducation(request.education()));
        }
        if (request.experience() != null) {
            profile.replaceExperienceEntries(mapExperience(request.experience()));
        }
        if (request.portfolioLinks() != null) {
            profile.replacePortfolioLinks(mapPortfolioLinks(request.portfolioLinks()));
        }
    }

    private List<StudentProfileSkill> mapSkills(List<String> skillNames) {
        List<StudentProfileSkill> skills = new ArrayList<>();
        for (String skillName : skillNames) {
            String normalizedSkillName = normalize(skillName);
            if (normalizedSkillName == null) {
                continue;
            }
            StudentProfileSkill skill = new StudentProfileSkill();
            skill.setName(normalizedSkillName);
            skills.add(skill);
        }
        return skills;
    }

    private List<StudentProfileEducation> mapEducation(List<EducationEntryRequest> educationRequests) {
        List<StudentProfileEducation> educationEntries = new ArrayList<>();
        for (EducationEntryRequest request : educationRequests) {
            StudentProfileEducation education = new StudentProfileEducation();
            education.setInstitution(normalize(request.institution()));
            education.setDegree(normalize(request.degree()));
            education.setFieldOfStudy(normalize(request.fieldOfStudy()));
            education.setStartDate(request.startDate());
            education.setEndDate(request.endDate());
            educationEntries.add(education);
        }
        return educationEntries;
    }

    private List<StudentProfileExperience> mapExperience(List<ExperienceEntryRequest> experienceRequests) {
        List<StudentProfileExperience> experienceEntries = new ArrayList<>();
        for (ExperienceEntryRequest request : experienceRequests) {
            StudentProfileExperience experience = new StudentProfileExperience();
            experience.setCompany(normalize(request.company()));
            experience.setTitle(normalize(request.title()));
            experience.setDescription(normalize(request.description()));
            experience.setStartDate(request.startDate());
            experience.setEndDate(request.endDate());
            experienceEntries.add(experience);
        }
        return experienceEntries;
    }

    private List<StudentProfilePortfolioLink> mapPortfolioLinks(List<PortfolioLinkRequest> portfolioLinkRequests) {
        List<StudentProfilePortfolioLink> portfolioLinks = new ArrayList<>();
        for (PortfolioLinkRequest request : portfolioLinkRequests) {
            StudentProfilePortfolioLink portfolioLink = new StudentProfilePortfolioLink();
            portfolioLink.setLabel(normalize(request.label()));
            portfolioLink.setUrl(normalize(request.url()));
            portfolioLinks.add(portfolioLink);
        }
        return portfolioLinks;
    }

    private String requireStudentContext(String userId, String userRole) {
        String normalizedUserId = normalize(userId);
        String normalizedRole = normalize(userRole);

        if (normalizedUserId == null || normalizedRole == null) {
            throw new ProfileServiceException(
                    "PROFILE_UNAUTHORIZED",
                    "Authenticated user context is required",
                    401
            );
        }
        if (!STUDENT_ROLE.equalsIgnoreCase(normalizedRole) && !JOB_SEEKER_ROLE.equalsIgnoreCase(normalizedRole)) {
            throw new ProfileServiceException(
                    "PROFILE_FORBIDDEN",
                    "Only students can access this operation",
                    403
            );
        }

        return normalizedUserId;
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }
}
