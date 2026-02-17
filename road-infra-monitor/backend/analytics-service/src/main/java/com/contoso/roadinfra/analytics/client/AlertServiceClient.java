package com.contoso.roadinfra.analytics.client;

import com.contoso.roadinfra.common.dto.ApiResponse;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Feign client for alert-service.
 */
@FeignClient(name = "alert-service", fallback = AlertServiceClientFallback.class)
public interface AlertServiceClient {

    @GetMapping("/api/v1/alerts/active")
    @CircuitBreaker(name = "alertService", fallbackMethod = "getActiveAlertsFallback")
    ApiResponse<List<Map<String, Object>>> getActiveAlerts();

    @GetMapping("/api/v1/alerts/asset/{assetId}")
    @CircuitBreaker(name = "alertService", fallbackMethod = "getAlertsByAssetFallback")
    ApiResponse<List<Map<String, Object>>> getAlertsByAsset(@PathVariable UUID assetId);

    @GetMapping("/api/v1/alerts/statistics")
    @CircuitBreaker(name = "alertService", fallbackMethod = "getAlertStatisticsFallback")
    ApiResponse<Map<String, Long>> getAlertStatistics();

    @GetMapping("/api/v1/alerts/escalation-stats")
    @CircuitBreaker(name = "alertService", fallbackMethod = "getEscalationStatsFallback")
    ApiResponse<Map<String, Object>> getEscalationStats();

    @GetMapping("/api/v1/alerts")
    @CircuitBreaker(name = "alertService", fallbackMethod = "getAllAlertsFallback")
    ApiResponse<List<Map<String, Object>>> getAllAlerts(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size);

    @GetMapping("/api/v1/alerts/critical/unacknowledged")
    @CircuitBreaker(name = "alertService", fallbackMethod = "getCriticalUnacknowledgedFallback")
    ApiResponse<List<Map<String, Object>>> getCriticalUnacknowledged();
}
