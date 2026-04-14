package com.jobportal.messagebroker.config;

import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.Declarable;
import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.core.Exchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BrokerStartupConfiguration {

    @Bean
    @ConditionalOnProperty(value = "spring.rabbitmq.dynamic", havingValue = "true", matchIfMissing = true)
    AmqpAdmin amqpAdmin(ConnectionFactory connectionFactory) {
        // RabbitAdmin gives the topology service a clear way to declare queues on startup.
        return new RabbitAdmin(connectionFactory);
    }

    @Bean
    @ConditionalOnProperty(value = "spring.rabbitmq.dynamic", havingValue = "true", matchIfMissing = true)
    ApplicationRunner topologyDeclarer(AmqpAdmin amqpAdmin, Declarables notificationTopology) {
        return args -> notificationTopology.getDeclarables().forEach(declarable -> declare(amqpAdmin, declarable));
    }

    private void declare(AmqpAdmin amqpAdmin, Declarable declarable) {
        if (declarable instanceof Queue queue) {
            amqpAdmin.declareQueue(queue);
            return;
        }

        if (declarable instanceof Exchange exchange) {
            amqpAdmin.declareExchange(exchange);
            return;
        }

        if (declarable instanceof org.springframework.amqp.core.Binding binding) {
            amqpAdmin.declareBinding(binding);
        }
    }
}
