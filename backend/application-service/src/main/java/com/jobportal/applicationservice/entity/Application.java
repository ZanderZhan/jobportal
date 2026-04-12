package com.jobportal.applicationservice.entity;

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
        name = "applications",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_applications_student_job", columnNames = {"student_id", "job_id"})
        }
)
public class Application {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "student_id", nullable = false, length = 255)
    private String studentId;

    @Column(name = "job_id", nullable = false)
    private Long jobId;

    @Column(name = "employer_id_snapshot", length = 255)
    private String employerIdSnapshot;

    @Column(name = "job_title_snapshot", nullable = false, length = 255)
    private String jobTitleSnapshot;

    @Column(name = "resume_reference", nullable = false, length = 500)
    private String resumeReference;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private ApplicationStatus status;

    @Column(name = "submitted_at", nullable = false)
    private LocalDateTime submittedAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "application", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("createdAt ASC, id ASC")
    private final List<ApplicationTimelineEntry> timelineEntries = new ArrayList<>();

    protected Application() {
    }

    public static Application createSubmitted(
            String studentId,
            Long jobId,
            String employerIdSnapshot,
            String jobTitleSnapshot,
            String resumeReference,
            String changedBy) {
        LocalDateTime now = LocalDateTime.now();
        Application application = new Application();
        application.studentId = studentId;
        application.jobId = jobId;
        application.employerIdSnapshot = employerIdSnapshot;
        application.jobTitleSnapshot = jobTitleSnapshot;
        application.resumeReference = resumeReference;
        application.status = ApplicationStatus.SUBMITTED;
        application.submittedAt = now;
        application.updatedAt = now;
        application.addTimelineEntry(null, ApplicationStatus.SUBMITTED, changedBy, "Application submitted");
        return application;
    }

    public void addTimelineEntry(
            ApplicationStatus oldStatus,
            ApplicationStatus newStatus,
            String changedBy,
            String reason) {
        timelineEntries.add(new ApplicationTimelineEntry(this, oldStatus, newStatus, changedBy, reason));
    }

    public void withdraw(String changedBy, String reason) {
        ApplicationStatus previousStatus = status;
        status = ApplicationStatus.WITHDRAWN;
        updatedAt = LocalDateTime.now();
        addTimelineEntry(previousStatus, ApplicationStatus.WITHDRAWN, changedBy, reason);
    }

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (submittedAt == null) {
            submittedAt = now;
        }
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public String getStudentId() {
        return studentId;
    }

    public Long getJobId() {
        return jobId;
    }

    public String getEmployerIdSnapshot() {
        return employerIdSnapshot;
    }

    public String getJobTitleSnapshot() {
        return jobTitleSnapshot;
    }

    public String getResumeReference() {
        return resumeReference;
    }

    public ApplicationStatus getStatus() {
        return status;
    }

    public LocalDateTime getSubmittedAt() {
        return submittedAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public List<ApplicationTimelineEntry> getTimelineEntries() {
        return timelineEntries;
    }
}
