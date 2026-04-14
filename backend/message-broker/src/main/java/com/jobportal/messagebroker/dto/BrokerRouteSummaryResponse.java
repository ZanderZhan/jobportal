package com.jobportal.messagebroker.dto;

public record BrokerRouteSummaryResponse(
        String routingKey,
        String queue,
        String retryRoutingKey,
        String retryQueue,
        String deadLetterQueue,
        String purpose
) {
}
