package com.jobportal.applicationservice.repository;

import com.jobportal.applicationservice.entity.Application;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ApplicationRepository extends JpaRepository<Application, Long> {

    boolean existsByStudentIdAndJobId(String studentId, Long jobId);

    List<Application> findAllByStudentIdOrderBySubmittedAtDesc(String studentId);

    Optional<Application> findByIdAndStudentId(Long id, String studentId);
}
