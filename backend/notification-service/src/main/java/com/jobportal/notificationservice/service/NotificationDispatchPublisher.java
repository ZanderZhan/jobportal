package com.jobportal.notificationservice.service;

import com.jobportal.notificationservice.dto.NotificationDispatchEvent;

public interface NotificationDispatchPublisher {

    void publish(NotificationDispatchEvent event);
}
