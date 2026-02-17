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
 * Feign client for sensor-service.
 */
@FeignClient(name = "sensor-service", fallback = SensorServiceClientFallback.class)
public interface SensorServiceClient {

    @GetMapping("/api/v1/sensors")
    @CircuitBreaker(name = "sensorService", fallbackMethod = "getAllSensorsFallback")
    ApiResponse<List<Map<String, Object>>> getAllSensors(@RequestParam(required = false) Integer page,
                                                          @RequestParam(required = false) Integer size);

    @GetMapping("/api/v1/sensors/{id}")
    @CircuitBreaker(name = "sensorService", fallbackMethod = "getSensorByIdFallback")
    ApiResponse<Map<String, Object>> getSensorById(@PathVariable UUID id);

    @GetMapping("/api/v1/sensors/asset/{assetId}")
    @CircuitBreaker(name = "sensorService", fallbackMethod = "getSensorsByAssetFallback")
    ApiResponse<List<Map<String, Object>>> getSensorsByAsset(@PathVariable UUID assetId);

    @GetMapping("/api/v1/sensors/status-counts")
    @CircuitBreaker(name = "sensorService", fallbackMethod = "getSensorStatusCountsFallback")
    ApiResponse<Map<String, Long>> getSensorStatusCounts();

    @GetMapping("/api/v1/readings/latest")
    @CircuitBreaker(name = "sensorService", fallbackMethod = "getLatestReadingsFallback")
    ApiResponse<List<Map<String, Object>>> getLatestReadings(@RequestParam(required = false) UUID assetId);

    @GetMapping("/api/v1/sensors/statistics")
    @CircuitBreaker(name = "sensorService", fallbackMethod = "getSensorStatisticsFallback")
    ApiResponse<Map<String, Object>> getSensorStatistics();
}
