package com.jobportal.messagebroker.service;

import com.jobportal.messagebroker.config.BrokerTopology;
import com.jobportal.messagebroker.dto.BrokerRouteSummaryResponse;
import com.jobportal.messagebroker.dto.BrokerTopologySummaryResponse;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BrokerTopologyCatalogService {

    public BrokerTopologySummaryResponse getTopologySummary() {
        return new BrokerTopologySummaryResponse(
                BrokerTopology.EVENTS_EXCHANGE,
                BrokerTopology.DEAD_LETTER_EXCHANGE,
                List.of(
                        route(
                                BrokerTopology.APPLICATION_SUBMITTED_ROUTING_KEY,
                                BrokerTopology.APPLICATION_SUBMITTED_QUEUE,
                                BrokerTopology.APPLICATION_SUBMITTED_RETRY_ROUTING_KEY,
                                BrokerTopology.APPLICATION_SUBMITTED_RETRY_QUEUE,
                                BrokerTopology.APPLICATION_SUBMITTED_DLQ,
                                "Sent after a student submits an application."
                        ),
                        route(
                                BrokerTopology.APPLICATION_STATUS_CHANGED_ROUTING_KEY,
                                BrokerTopology.APPLICATION_STATUS_CHANGED_QUEUE,
                                BrokerTopology.APPLICATION_STATUS_CHANGED_RETRY_ROUTING_KEY,
                                BrokerTopology.APPLICATION_STATUS_CHANGED_RETRY_QUEUE,
                                BrokerTopology.APPLICATION_STATUS_CHANGED_DLQ,
                                "Used when an application moves to a new status."
                        ),
                        route(
                                BrokerTopology.APPLICATION_WITHDRAWN_ROUTING_KEY,
                                BrokerTopology.APPLICATION_WITHDRAWN_QUEUE,
                                BrokerTopology.APPLICATION_WITHDRAWN_RETRY_ROUTING_KEY,
                                BrokerTopology.APPLICATION_WITHDRAWN_RETRY_QUEUE,
                                BrokerTopology.APPLICATION_WITHDRAWN_DLQ,
                                "Used when a student withdraws an application."
                        ),
                        route(
                                BrokerTopology.JOB_POSTED_ROUTING_KEY,
                                BrokerTopology.JOB_POSTED_QUEUE,
                                BrokerTopology.JOB_POSTED_RETRY_ROUTING_KEY,
                                BrokerTopology.JOB_POSTED_RETRY_QUEUE,
                                BrokerTopology.JOB_POSTED_DLQ,
                                "Reserved for job publication side effects."
                        ),
                        route(
                                BrokerTopology.EMPLOYER_VERIFIED_ROUTING_KEY,
                                BrokerTopology.EMPLOYER_VERIFIED_QUEUE,
                                BrokerTopology.EMPLOYER_VERIFIED_RETRY_ROUTING_KEY,
                                BrokerTopology.EMPLOYER_VERIFIED_RETRY_QUEUE,
                                BrokerTopology.EMPLOYER_VERIFIED_DLQ,
                                "Reserved for employer verification follow-up actions."
                        )
                )
        );
    }

    private BrokerRouteSummaryResponse route(
            String routingKey,
            String queue,
            String retryRoutingKey,
            String retryQueue,
            String deadLetterQueue,
            String purpose
    ) {
        return new BrokerRouteSummaryResponse(routingKey, queue, retryRoutingKey, retryQueue, deadLetterQueue, purpose);
    }
}
