package com.contoso.roadinfra.asset.client;

import com.contoso.roadinfra.common.dto.ApiResponse;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Feign client for communicating with the sensor-service.
 * Uses circuit breaker pattern for resilience.
 */
@FeignClient(name = "sensor-service", fallback = SensorServiceClientFallback.class)
public interface SensorServiceClient {

    /**
     * Get all sensors for a specific asset.
     */
    @GetMapping("/api/v1/sensors/asset/{assetId}")
    @CircuitBreaker(name = "sensorService", fallbackMethod = "getSensorsByAssetFallback")
    ApiResponse<List<SensorInfo>> getSensorsByAssetId(@PathVariable("assetId") UUID assetId);

    /**
     * Get sensor count for an asset.
     */
    @GetMapping("/api/v1/sensors/asset/{assetId}/count")
    @CircuitBreaker(name = "sensorService", fallbackMethod = "getSensorCountFallback")
    ApiResponse<Long> getSensorCountByAssetId(@PathVariable("assetId") UUID assetId);

    /**
     * Simplified sensor info returned from sensor-service.
     */
    record SensorInfo(
            UUID id,
            String sensorCode,
            String sensorType,
            String status,
            Double latitude,
            Double longitude
    ) {}

    // Fallback methods
    default ApiResponse<List<SensorInfo>> getSensorsByAssetFallback(UUID assetId, Exception e) {
        return ApiResponse.<List<SensorInfo>>builder()
                .status("FALLBACK")
                .message("Sensor service unavailable")
                .data(Collections.emptyList())
                .build();
    }

    default ApiResponse<Long> getSensorCountFallback(UUID assetId, Exception e) {
        return ApiResponse.<Long>builder()
                .status("FALLBACK")
                .message("Sensor service unavailable")
                .data(0L)
                .build();
    }
}
