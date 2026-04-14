package com.jobportal.notificationservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class NotificationServiceApplication {

    public static void main(String[] args) {
        // Starts the notification service and its event consumers.
        SpringApplication.run(NotificationServiceApplication.class, args);
    }
}
