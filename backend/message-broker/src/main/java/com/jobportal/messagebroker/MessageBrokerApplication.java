package com.jobportal.messagebroker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class MessageBrokerApplication {

    public static void main(String[] args) {
        // Starts the service that prepares the messaging topology for other services.
        SpringApplication.run(MessageBrokerApplication.class, args);
    }
}
