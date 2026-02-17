package com.contoso.roadinfra.analytics.client;

import com.contoso.roadinfra.common.dto.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Fallback implementation for MonitoringServiceClient.
 */
@Component
@Slf4j
public class MonitoringServiceClientFallback implements MonitoringServiceClient {

    @Override
    public ApiResponse<Map<String, Object>> getAssetHealth(UUID assetId) {
        log.warn("Fallback: monitoring-service unavailable for getAssetHealth: {}", assetId);
        return ApiResponse.success(Map.of(
                "assetId", assetId.toString(),
                "healthScore", 0.0,
                "healthStatus", "UNKNOWN"
        ));
    }

    @Override
    public ApiResponse<List<Map<String, Object>>> getAllAssetHealth(String assetType, String status) {
        log.warn("Fallback: monitoring-service unavailable for getAllAssetHealth");
        return ApiResponse.success(Collections.emptyList());
    }

    @Override
    public ApiResponse<Map<String, Object>> getCorridorSummary() {
        log.warn("Fallback: monitoring-service unavailable for getCorridorSummary");
        return ApiResponse.success(Map.of(
                "totalAssets", 0,
                "healthyCount", 0,
                "warningCount", 0,
                "criticalCount", 0,
                "averageHealthScore", 0.0,
                "sensorUptime", 0.0
        ));
    }

    @Override
    public ApiResponse<List<Map<String, Object>>> getHealthHistory(UUID assetId, String from, String to) {
        log.warn("Fallback: monitoring-service unavailable for getHealthHistory: {}", assetId);
        return ApiResponse.success(Collections.emptyList());
    }

    @Override
    public ApiResponse<List<Map<String, Object>>> getAssetsRequiringAttention() {
        log.warn("Fallback: monitoring-service unavailable for getAssetsRequiringAttention");
        return ApiResponse.success(Collections.emptyList());
    }
}
