package com.jobportal.notificationservice.dto;

import com.jobportal.notificationservice.entity.DeliveryChannel;
import com.jobportal.notificationservice.entity.DeliveryStatus;

import java.time.Instant;

public record DeliveryRecordResponse(
        Long id,
        DeliveryChannel channel,
        Integer attemptNo,
        String provider,
        DeliveryStatus status,
        String errorMessage,
        Instant sentAt
) {
}
