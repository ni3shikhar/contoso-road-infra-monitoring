package com.contoso.roadinfra.gateway.config;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Gateway configuration with route definitions and circuit breakers.
 * Routes defined here supplement those in application.yml with additional
 * filters like circuit breakers and retry logic.
 */
@Configuration
public class GatewayConfig {

    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
                // Auth Service - no circuit breaker/retry for auth to avoid token issues
                .route("auth-service-custom", r -> r
                        .path("/api/v1/auth/**")
                        .filters(f -> f
                                .circuitBreaker(c -> c
                                        .setName("authServiceCircuitBreaker")
                                        .setFallbackUri("forward:/fallback/auth")))
                        .uri("lb://AUTH-SERVICE"))

                // Sensor Service with circuit breaker and retry
                .route("sensor-service-custom", r -> r
                        .path("/api/v1/sensors/**")
                        .filters(f -> f
                                .circuitBreaker(c -> c
                                        .setName("sensorServiceCircuitBreaker")
                                        .setFallbackUri("forward:/fallback/sensors"))
                                .retry(retryConfig -> retryConfig.setRetries(3)))
                        .uri("lb://SENSOR-SERVICE"))

                // Asset Service with circuit breaker and retry
                .route("asset-service-custom", r -> r
                        .path("/api/v1/assets/**")
                        .filters(f -> f
                                .circuitBreaker(c -> c
                                        .setName("assetServiceCircuitBreaker")
                                        .setFallbackUri("forward:/fallback/assets"))
                                .retry(retryConfig -> retryConfig.setRetries(3)))
                        .uri("lb://ASSET-SERVICE"))

                // Monitoring Service with circuit breaker and retry
                .route("monitoring-service-custom", r -> r
                        .path("/api/v1/monitoring/**", "/api/v1/health-status/**")
                        .filters(f -> f
                                .circuitBreaker(c -> c
                                        .setName("monitoringServiceCircuitBreaker")
                                        .setFallbackUri("forward:/fallback/monitoring"))
                                .retry(retryConfig -> retryConfig.setRetries(3)))
                        .uri("lb://MONITORING-SERVICE"))

                // Alert Service with circuit breaker and retry
                .route("alert-service-custom", r -> r
                        .path("/api/v1/alerts/**")
                        .filters(f -> f
                                .circuitBreaker(c -> c
                                        .setName("alertServiceCircuitBreaker")
                                        .setFallbackUri("forward:/fallback/alerts"))
                                .retry(retryConfig -> retryConfig.setRetries(3)))
                        .uri("lb://ALERT-SERVICE"))

                // Analytics Service with circuit breaker and retry
                .route("analytics-service-custom", r -> r
                        .path("/api/v1/analytics/**", "/api/v1/kpis/**")
                        .filters(f -> f
                                .circuitBreaker(c -> c
                                        .setName("analyticsServiceCircuitBreaker")
                                        .setFallbackUri("forward:/fallback/analytics"))
                                .retry(retryConfig -> retryConfig.setRetries(3)))
                        .uri("lb://ANALYTICS-SERVICE"))

                .build();
    }
}
