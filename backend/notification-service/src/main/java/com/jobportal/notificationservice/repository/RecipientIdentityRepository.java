package com.jobportal.notificationservice.repository;

import com.jobportal.notificationservice.entity.RecipientIdentity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RecipientIdentityRepository extends JpaRepository<RecipientIdentity, String> {
}
