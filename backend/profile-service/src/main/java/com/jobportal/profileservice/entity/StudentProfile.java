package com.jobportal.profileservice.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        name = "student_profiles",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_student_profiles_user_id", columnNames = "user_id")
        }
)
public class StudentProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, length = 255)
    private String userId;

    @Column(length = 255)
    private String headline;

    @Column(columnDefinition = "TEXT")
    private String bio;

    @Column(length = 255)
    private String location;

    @Column(length = 64)
    private String phone;

    @Column(name = "resume_reference", length = 500)
    private String resumeReference;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private ProfileVisibility visibility = ProfileVisibility.PRIVATE;

    @Column(name = "job_search_status", length = 64)
    private String jobSearchStatus;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "profile", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    private final List<StudentProfileSkill> skills = new ArrayList<>();

    @OneToMany(mappedBy = "profile", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    private final List<StudentProfileEducation> educationEntries = new ArrayList<>();

    @OneToMany(mappedBy = "profile", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    private final List<StudentProfileExperience> experienceEntries = new ArrayList<>();

    @OneToMany(mappedBy = "profile", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    private final List<StudentProfilePortfolioLink> portfolioLinks = new ArrayList<>();

    protected StudentProfile() {
    }

    public static StudentProfile createEmpty(String userId) {
        StudentProfile profile = new StudentProfile();
        profile.userId = userId;
        profile.visibility = ProfileVisibility.PRIVATE;
        return profile;
    }

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public void replaceSkills(List<StudentProfileSkill> newSkills) {
        skills.clear();
        for (StudentProfileSkill skill : newSkills) {
            skill.setProfile(this);
            skills.add(skill);
        }
    }

    public void replaceEducationEntries(List<StudentProfileEducation> newEducationEntries) {
        educationEntries.clear();
        for (StudentProfileEducation educationEntry : newEducationEntries) {
            educationEntry.setProfile(this);
            educationEntries.add(educationEntry);
        }
    }

    public void replaceExperienceEntries(List<StudentProfileExperience> newExperienceEntries) {
        experienceEntries.clear();
        for (StudentProfileExperience experienceEntry : newExperienceEntries) {
            experienceEntry.setProfile(this);
            experienceEntries.add(experienceEntry);
        }
    }

    public void replacePortfolioLinks(List<StudentProfilePortfolioLink> newPortfolioLinks) {
        portfolioLinks.clear();
        for (StudentProfilePortfolioLink portfolioLink : newPortfolioLinks) {
            portfolioLink.setProfile(this);
            portfolioLinks.add(portfolioLink);
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getHeadline() {
        return headline;
    }

    public void setHeadline(String headline) {
        this.headline = headline;
    }

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getResumeReference() {
        return resumeReference;
    }

    public void setResumeReference(String resumeReference) {
        this.resumeReference = resumeReference;
    }

    public ProfileVisibility getVisibility() {
        return visibility;
    }

    public void setVisibility(ProfileVisibility visibility) {
        this.visibility = visibility == null ? ProfileVisibility.PRIVATE : visibility;
    }

    public String getJobSearchStatus() {
        return jobSearchStatus;
    }

    public void setJobSearchStatus(String jobSearchStatus) {
        this.jobSearchStatus = jobSearchStatus;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public List<StudentProfileSkill> getSkills() {
        return skills;
    }

    public List<StudentProfileEducation> getEducationEntries() {
        return educationEntries;
    }

    public List<StudentProfileExperience> getExperienceEntries() {
        return experienceEntries;
    }

    public List<StudentProfilePortfolioLink> getPortfolioLinks() {
        return portfolioLinks;
    }
}
