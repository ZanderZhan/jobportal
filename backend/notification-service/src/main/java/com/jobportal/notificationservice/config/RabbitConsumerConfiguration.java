package com.jobportal.notificationservice.config;

import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConsumerConfiguration {

    @Bean
    MessageConverter rabbitMessageConverter() {
        // Shared JSON conversion keeps payload handling simple across listeners.
        return new JacksonJsonMessageConverter();
    }
}
