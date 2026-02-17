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
 * Feign client for monitoring-service.
 */
@FeignClient(name = "monitoring-service", fallback = MonitoringServiceClientFallback.class)
public interface MonitoringServiceClient {

    @GetMapping("/api/v1/monitoring/health/{assetId}")
    @CircuitBreaker(name = "monitoringService", fallbackMethod = "getAssetHealthFallback")
    ApiResponse<Map<String, Object>> getAssetHealth(@PathVariable UUID assetId);

    @GetMapping("/api/v1/monitoring/health")
    @CircuitBreaker(name = "monitoringService", fallbackMethod = "getAllAssetHealthFallback")
    ApiResponse<List<Map<String, Object>>> getAllAssetHealth(
            @RequestParam(required = false) String assetType,
            @RequestParam(required = false) String status);

    @GetMapping("/api/v1/monitoring/corridor-summary")
    @CircuitBreaker(name = "monitoringService", fallbackMethod = "getCorridorSummaryFallback")
    ApiResponse<Map<String, Object>> getCorridorSummary();

    @GetMapping("/api/v1/monitoring/health/{assetId}/history")
    @CircuitBreaker(name = "monitoringService", fallbackMethod = "getHealthHistoryFallback")
    ApiResponse<List<Map<String, Object>>> getHealthHistory(
            @PathVariable UUID assetId,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to);

    @GetMapping("/api/v1/monitoring/attention-required")
    @CircuitBreaker(name = "monitoringService", fallbackMethod = "getAssetsRequiringAttentionFallback")
    ApiResponse<List<Map<String, Object>>> getAssetsRequiringAttention();
}
