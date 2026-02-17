package com.contoso.roadinfra.analytics.client;

import com.contoso.roadinfra.common.dto.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Fallback implementation for AlertServiceClient.
 */
@Component
@Slf4j
public class AlertServiceClientFallback implements AlertServiceClient {

    @Override
    public ApiResponse<List<Map<String, Object>>> getActiveAlerts() {
        log.warn("Fallback: alert-service unavailable for getActiveAlerts");
        return ApiResponse.success(Collections.emptyList());
    }

    @Override
    public ApiResponse<List<Map<String, Object>>> getAlertsByAsset(UUID assetId) {
        log.warn("Fallback: alert-service unavailable for getAlertsByAsset: {}", assetId);
        return ApiResponse.success(Collections.emptyList());
    }

    @Override
    public ApiResponse<Map<String, Long>> getAlertStatistics() {
        log.warn("Fallback: alert-service unavailable for getAlertStatistics");
        return ApiResponse.success(Map.of(
                "critical", 0L,
                "high", 0L,
                "medium", 0L,
                "low", 0L,
                "info", 0L
        ));
    }

    @Override
    public ApiResponse<Map<String, Object>> getEscalationStats() {
        log.warn("Fallback: alert-service unavailable for getEscalationStats");
        return ApiResponse.success(Map.of(
                "totalActive", 0,
                "escalated", 0,
                "criticalEscalated", 0
        ));
    }

    @Override
    public ApiResponse<List<Map<String, Object>>> getAllAlerts(Integer page, Integer size) {
        log.warn("Fallback: alert-service unavailable for getAllAlerts");
        return ApiResponse.success(Collections.emptyList());
    }

    @Override
    public ApiResponse<List<Map<String, Object>>> getCriticalUnacknowledged() {
        log.warn("Fallback: alert-service unavailable for getCriticalUnacknowledged");
        return ApiResponse.success(Collections.emptyList());
    }
}
