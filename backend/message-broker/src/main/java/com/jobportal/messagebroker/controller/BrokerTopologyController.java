package com.jobportal.messagebroker.controller;

import com.jobportal.messagebroker.dto.BrokerTopologySummaryResponse;
import com.jobportal.messagebroker.service.BrokerTopologyCatalogService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/broker")
@ConditionalOnProperty(value = "broker.topology.endpoint-enabled", havingValue = "true")
public class BrokerTopologyController {

    private final BrokerTopologyCatalogService brokerTopologyCatalogService;

    public BrokerTopologyController(BrokerTopologyCatalogService brokerTopologyCatalogService) {
        this.brokerTopologyCatalogService = brokerTopologyCatalogService;
    }

    @GetMapping("/topology")
    public BrokerTopologySummaryResponse getTopologySummary() {
        // This makes it easy to review the broker setup without opening the code first.
        return brokerTopologyCatalogService.getTopologySummary();
    }
}
