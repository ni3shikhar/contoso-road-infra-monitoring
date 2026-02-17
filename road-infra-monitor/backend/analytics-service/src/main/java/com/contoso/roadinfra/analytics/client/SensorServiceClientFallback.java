package com.contoso.roadinfra.analytics.client;

import com.contoso.roadinfra.common.dto.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Fallback implementation for SensorServiceClient.
 */
@Component
@Slf4j
public class SensorServiceClientFallback implements SensorServiceClient {

    @Override
    public ApiResponse<List<Map<String, Object>>> getAllSensors(Integer page, Integer size) {
        log.warn("Fallback: sensor-service unavailable for getAllSensors");
        return ApiResponse.success(Collections.emptyList());
    }

    @Override
    public ApiResponse<Map<String, Object>> getSensorById(UUID id) {
        log.warn("Fallback: sensor-service unavailable for getSensorById: {}", id);
        return ApiResponse.success(Collections.emptyMap());
    }

    @Override
    public ApiResponse<List<Map<String, Object>>> getSensorsByAsset(UUID assetId) {
        log.warn("Fallback: sensor-service unavailable for getSensorsByAsset: {}", assetId);
        return ApiResponse.success(Collections.emptyList());
    }

    @Override
    public ApiResponse<Map<String, Long>> getSensorStatusCounts() {
        log.warn("Fallback: sensor-service unavailable for getSensorStatusCounts");
        return ApiResponse.success(Map.of(
                "ACTIVE", 0L,
                "INACTIVE", 0L,
                "FAULTY", 0L,
                "UNKNOWN", 0L
        ));
    }

    @Override
    public ApiResponse<List<Map<String, Object>>> getLatestReadings(UUID assetId) {
        log.warn("Fallback: sensor-service unavailable for getLatestReadings");
        return ApiResponse.success(Collections.emptyList());
    }

    @Override
    public ApiResponse<Map<String, Object>> getSensorStatistics() {
        log.warn("Fallback: sensor-service unavailable for getSensorStatistics");
        return ApiResponse.success(Map.of(
                "totalSensors", 0,
                "activeSensors", 0,
                "averageUptime", 0.0
        ));
    }
}
