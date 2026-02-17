package com.contoso.roadinfra.analytics.client;

import com.contoso.roadinfra.common.dto.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Fallback implementation for AssetServiceClient.
 */
@Component
@Slf4j
public class AssetServiceClientFallback implements AssetServiceClient {

    @Override
    public ApiResponse<List<Map<String, Object>>> getAllAssets(Integer page, Integer size) {
        log.warn("Fallback: asset-service unavailable for getAllAssets");
        return ApiResponse.success(Collections.emptyList());
    }

    @Override
    public ApiResponse<Map<String, Object>> getAssetById(UUID id) {
        log.warn("Fallback: asset-service unavailable for getAssetById: {}", id);
        return ApiResponse.success(Collections.emptyMap());
    }

    @Override
    public ApiResponse<List<Map<String, Object>>> getAssetsByType(String type) {
        log.warn("Fallback: asset-service unavailable for getAssetsByType: {}", type);
        return ApiResponse.success(Collections.emptyList());
    }

    @Override
    public ApiResponse<Map<String, Object>> getAssetStatistics() {
        log.warn("Fallback: asset-service unavailable for getAssetStatistics");
        return ApiResponse.success(Map.of(
                "totalAssets", 0,
                "activeAssets", 0,
                "underMaintenance", 0
        ));
    }

    @Override
    public ApiResponse<Map<String, Long>> getAssetCountByType() {
        log.warn("Fallback: asset-service unavailable for getAssetCountByType");
        return ApiResponse.success(Collections.emptyMap());
    }

    @Override
    public ApiResponse<Map<String, Long>> getAssetCountByStatus() {
        log.warn("Fallback: asset-service unavailable for getAssetCountByStatus");
        return ApiResponse.success(Map.of(
                "OPERATIONAL", 0L,
                "MAINTENANCE", 0L,
                "DECOMMISSIONED", 0L
        ));
    }
}
