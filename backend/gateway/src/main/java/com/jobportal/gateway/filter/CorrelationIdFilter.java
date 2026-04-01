package com.jobportal.gateway.filter;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Component
public class CorrelationIdFilter implements GlobalFilter, Ordered {

    private static final String DEFAULT_HEADER = "X-Request-ID";

    @Value("${gateway.correlation-id-header:" + DEFAULT_HEADER + "}")
    private String correlationIdHeader;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String existingCorrelationId = exchange.getRequest().getHeaders().getFirst(correlationIdHeader);

        final String correlationId;
        if (existingCorrelationId == null || existingCorrelationId.isEmpty()) {
            correlationId = UUID.randomUUID().toString();
        } else {
            correlationId = existingCorrelationId;
        }

        ServerWebExchange mutatedExchange = exchange.mutate()
            .request(builder -> builder.header(correlationIdHeader, correlationId).build())
            .build();

        return chain.filter(mutatedExchange)
            .then(Mono.fromRunnable(() -> {
                mutatedExchange.getResponse().getHeaders().add(correlationIdHeader, correlationId);
            }));
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

    public String getCorrelationIdHeader() {
        return correlationIdHeader;
    }
}
