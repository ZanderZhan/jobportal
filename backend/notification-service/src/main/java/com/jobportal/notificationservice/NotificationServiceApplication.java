package com.jobportal.notificationservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@ConfigurationPropertiesScan
public class NotificationServiceApplication {

    public static void main(String[] args) {
        // Starts the notification service and its event consumers.
        SpringApplication.run(NotificationServiceApplication.class, args);
    }
}
