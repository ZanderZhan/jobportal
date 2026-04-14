package com.jobportal.messagebroker.service;

import com.jobportal.messagebroker.config.BrokerTopology;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BrokerTopologyCatalogServiceTest {

    private final BrokerTopologyCatalogService brokerTopologyCatalogService = new BrokerTopologyCatalogService();

    @Test
    void shouldExposeAllExpectedRoutes() {
        var response = brokerTopologyCatalogService.getTopologySummary();

        assertEquals(BrokerTopology.EVENTS_EXCHANGE, response.exchange());
        assertEquals(BrokerTopology.DEAD_LETTER_EXCHANGE, response.deadLetterExchange());
        assertEquals(5, response.routes().size());
        assertTrue(response.routes().stream().anyMatch(route ->
                BrokerTopology.APPLICATION_WITHDRAWN_ROUTING_KEY.equals(route.routingKey())
                        && BrokerTopology.APPLICATION_WITHDRAWN_QUEUE.equals(route.queue())
        ));
    }
}
