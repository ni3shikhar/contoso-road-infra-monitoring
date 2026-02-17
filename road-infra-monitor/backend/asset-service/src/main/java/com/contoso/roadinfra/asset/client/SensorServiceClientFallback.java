package com.contoso.roadinfra.asset.client;

import com.contoso.roadinfra.common.dto.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Fallback implementation for SensorServiceClient when the sensor-service is unavailable.
 */
@Component
@Slf4j
public class SensorServiceClientFallback implements SensorServiceClient {

    @Override
    public ApiResponse<List<SensorInfo>> getSensorsByAssetId(UUID assetId) {
        log.warn("Fallback triggered: Unable to retrieve sensors for asset {}", assetId);
        return ApiResponse.<List<SensorInfo>>builder()
                .status("FALLBACK")
                .message("Sensor service is currently unavailable. Please try again later.")
                .data(Collections.emptyList())
                .build();
    }

    @Override
    public ApiResponse<Long> getSensorCountByAssetId(UUID assetId) {
        log.warn("Fallback triggered: Unable to retrieve sensor count for asset {}", assetId);
        return ApiResponse.<Long>builder()
                .status("FALLBACK")
                .message("Sensor service is currently unavailable. Please try again later.")
                .data(0L)
                .build();
    }
}
