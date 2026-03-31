package com.jobportal.jobservice.repository;

import com.jobportal.jobservice.entity.Job;
import com.jobportal.jobservice.entity.Job.EmploymentType;
import com.jobportal.jobservice.entity.Job.JobStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface JobRepository extends JpaRepository<Job, Long>, JpaSpecificationExecutor<Job> {

    Page<Job> findByStatus(JobStatus status, Pageable pageable);

    Page<Job> findByCompanyContainingIgnoreCase(String company, Pageable pageable);

    Page<Job> findByLocationContainingIgnoreCase(String location, Pageable pageable);

    Page<Job> findByEmploymentType(EmploymentType employmentType, Pageable pageable);

    @Query("SELECT j FROM Job j WHERE " +
           "(:title IS NULL OR LOWER(j.title) LIKE LOWER(CONCAT('%', :title, '%'))) AND " +
           "(:company IS NULL OR LOWER(j.company) LIKE LOWER(CONCAT('%', :company, '%'))) AND " +
           "(:location IS NULL OR LOWER(j.location) LIKE LOWER(CONCAT('%', :location, '%'))) AND " +
           "(:employmentType IS NULL OR j.employmentType = :employmentType) AND " +
           "(:salaryMin IS NULL OR j.salaryMin >= :salaryMin) AND " +
           "(:salaryMax IS NULL OR j.salaryMax <= :salaryMax) AND " +
           "(:status IS NULL OR j.status = :status)")
    Page<Job> searchJobs(
            @Param("title") String title,
            @Param("company") String company,
            @Param("location") String location,
            @Param("employmentType") EmploymentType employmentType,
            @Param("salaryMin") BigDecimal salaryMin,
            @Param("salaryMax") BigDecimal salaryMax,
            @Param("status") JobStatus status,
            Pageable pageable
    );

    List<Job> findByStatusOrderByCreatedAtDesc(JobStatus status);
}
