package com.contoso.roadinfra.gateway.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Instant;

@Component
public class LoggingGatewayFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(LoggingGatewayFilter.class);

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        long startTime = Instant.now().toEpochMilli();
        String method = exchange.getRequest().getMethod().name();
        String path = exchange.getRequest().getPath().value();
        String correlationId = exchange.getRequest().getHeaders().getFirst("X-Correlation-ID");

        log.info("Incoming request: {} {} [correlationId={}]", method, path, correlationId);

        return chain.filter(exchange)
                .then(Mono.fromRunnable(() -> {
                    long duration = Instant.now().toEpochMilli() - startTime;
                    HttpStatusCode statusCode = exchange.getResponse().getStatusCode();
                    log.info("Outgoing response: {} {} - {} - {}ms [correlationId={}]",
                            method, path, statusCode, duration, correlationId);
                }));
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 1;
    }
}
