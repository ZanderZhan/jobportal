package com.jobportal.messagebroker.config;

import com.jobportal.messagebroker.contract.ApplicationStatusChangedEvent;
import com.jobportal.messagebroker.contract.ApplicationWithdrawnEvent;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Declarable;
import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Collection;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class BrokerTopologyConfigurationTest {

    @Autowired
    private Declarables notificationTopology;

    @Autowired
    private MessageConverter rabbitMessageConverter;

    @Test
    void shouldProvisionNotificationTopologyForRequiredRoutingKeys() {
        Collection<Declarable> declarables = notificationTopology.getDeclarables();

        assertTrue(declarables.stream().anyMatch(declarable ->
                declarable instanceof TopicExchange exchange
                        && BrokerTopology.EVENTS_EXCHANGE.equals(exchange.getName())
        ));
        assertTrue(declarables.stream().anyMatch(declarable ->
                declarable instanceof DirectExchange exchange
                        && BrokerTopology.DEAD_LETTER_EXCHANGE.equals(exchange.getName())
        ));

        Queue applicationStatusChangedQueue = declarables.stream()
                .filter(Queue.class::isInstance)
                .map(Queue.class::cast)
                .filter(queue -> BrokerTopology.APPLICATION_STATUS_CHANGED_QUEUE.equals(queue.getName()))
                .findFirst()
                .orElseThrow();

        assertEquals(
                BrokerTopology.DEAD_LETTER_EXCHANGE,
                applicationStatusChangedQueue.getArguments().get("x-dead-letter-exchange")
        );
        assertEquals(
                BrokerTopology.APPLICATION_STATUS_CHANGED_DLQ,
                applicationStatusChangedQueue.getArguments().get("x-dead-letter-routing-key")
        );

        assertTrue(declarables.stream().anyMatch(declarable ->
                declarable instanceof Binding binding
                        && BrokerTopology.APPLICATION_SUBMITTED_QUEUE.equals(binding.getDestination())
                        && BrokerTopology.APPLICATION_SUBMITTED_ROUTING_KEY.equals(binding.getRoutingKey())
        ));
        assertTrue(declarables.stream().anyMatch(declarable ->
                declarable instanceof Binding binding
                        && BrokerTopology.APPLICATION_STATUS_CHANGED_QUEUE.equals(binding.getDestination())
                        && BrokerTopology.APPLICATION_STATUS_CHANGED_ROUTING_KEY.equals(binding.getRoutingKey())
        ));
        assertTrue(declarables.stream().anyMatch(declarable ->
                declarable instanceof Binding binding
                        && BrokerTopology.APPLICATION_WITHDRAWN_QUEUE.equals(binding.getDestination())
                        && BrokerTopology.APPLICATION_WITHDRAWN_ROUTING_KEY.equals(binding.getRoutingKey())
        ));
        assertTrue(declarables.stream().anyMatch(declarable ->
                declarable instanceof Binding binding
                        && BrokerTopology.JOB_POSTED_QUEUE.equals(binding.getDestination())
                        && BrokerTopology.JOB_POSTED_ROUTING_KEY.equals(binding.getRoutingKey())
        ));
        assertTrue(declarables.stream().anyMatch(declarable ->
                declarable instanceof Binding binding
                        && BrokerTopology.EMPLOYER_VERIFIED_QUEUE.equals(binding.getDestination())
                        && BrokerTopology.EMPLOYER_VERIFIED_ROUTING_KEY.equals(binding.getRoutingKey())
        ));
    }

    @Test
    void shouldSerializeApplicationStatusChangedEventAsJson() {
        ApplicationStatusChangedEvent event = new ApplicationStatusChangedEvent(
                99L,
                "student-4",
                "employer-2",
                31L,
                "UNDER_REVIEW",
                "INTERVIEW",
                Instant.parse("2026-04-07T20:15:30Z")
        );

        MessageProperties messageProperties = new MessageProperties();
        Message message = rabbitMessageConverter.toMessage(event, messageProperties);

        String json = new String(message.getBody(), StandardCharsets.UTF_8);

        assertTrue(message.getMessageProperties().getContentType().startsWith("application/json"));
        assertTrue(json.contains("\"applicationId\":99"));
        assertTrue(json.contains("\"studentId\":\"student-4\""));
        assertTrue(json.contains("\"employerId\":\"employer-2\""));
        assertTrue(json.contains("\"jobId\":31"));
        assertTrue(json.contains("\"oldStatus\":\"UNDER_REVIEW\""));
        assertTrue(json.contains("\"newStatus\":\"INTERVIEW\""));
        assertTrue(json.contains("\"timestamp\":\"2026-04-07T20:15:30Z\""));
    }

    @Test
    void shouldSerializeApplicationWithdrawnEventAsJson() {
        ApplicationWithdrawnEvent event = new ApplicationWithdrawnEvent(
                77L,
                "student-7",
                19L,
                Instant.parse("2026-04-12T15:20:00Z")
        );

        MessageProperties messageProperties = new MessageProperties();
        Message message = rabbitMessageConverter.toMessage(event, messageProperties);

        String json = new String(message.getBody(), StandardCharsets.UTF_8);

        assertTrue(message.getMessageProperties().getContentType().startsWith("application/json"));
        assertTrue(json.contains("\"applicationId\":77"));
        assertTrue(json.contains("\"studentId\":\"student-7\""));
        assertTrue(json.contains("\"jobId\":19"));
        assertTrue(json.contains("\"timestamp\":\"2026-04-12T15:20:00Z\""));
    }
}
