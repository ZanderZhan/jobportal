package com.jobportal.messagebroker.dto;

import java.util.List;

public record BrokerTopologySummaryResponse(
        String exchange,
        String deadLetterExchange,
        List<BrokerRouteSummaryResponse> routes
) {
}
