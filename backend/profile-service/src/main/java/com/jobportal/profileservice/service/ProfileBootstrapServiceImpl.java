package com.jobportal.profileservice.service;

import com.jobportal.profileservice.entity.StudentProfile;
import com.jobportal.profileservice.repository.StudentProfileRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ProfileBootstrapServiceImpl implements ProfileBootstrapService {

    private final StudentProfileRepository studentProfileRepository;

    public ProfileBootstrapServiceImpl(StudentProfileRepository studentProfileRepository) {
        this.studentProfileRepository = studentProfileRepository;
    }

    @Override
    public StudentProfile getOrCreateStudentProfile(String userId) {
        return studentProfileRepository.findByUserId(userId)
                .orElseGet(() -> studentProfileRepository.save(StudentProfile.createEmpty(userId)));
    }
}
