package com.jobportal.notificationservice.repository;

import com.jobportal.notificationservice.entity.DeliveryRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DeliveryRecordRepository extends JpaRepository<DeliveryRecord, Long> {

    List<DeliveryRecord> findByNotificationIdOrderByAttemptNoAsc(Long notificationId);
}
