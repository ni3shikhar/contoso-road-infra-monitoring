package com.contoso.roadinfra.monitoring.controller;

import com.contoso.roadinfra.common.constants.AssetType;
import com.contoso.roadinfra.common.constants.HealthStatus;
import com.contoso.roadinfra.common.dto.ApiResponse;
import com.contoso.roadinfra.monitoring.dto.AssetHealthResponse;
import com.contoso.roadinfra.monitoring.dto.CorridorHealthSummary;
import com.contoso.roadinfra.monitoring.dto.HealthThresholdResponse;
import com.contoso.roadinfra.monitoring.dto.HealthThresholdUpdateRequest;
import com.contoso.roadinfra.monitoring.service.HealthMonitoringService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * REST controller for health monitoring endpoints.
 */
@RestController
@RequestMapping("/api/v1/health")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Health Monitoring", description = "Real-time asset health monitoring")
@SecurityRequirement(name = "bearerAuth")
public class HealthMonitoringController {

    private final HealthMonitoringService healthMonitoringService;

    // ================= Health Status Endpoints =================

    @GetMapping("/assets")
    @Operation(summary = "Get all asset health status", 
               description = "Get health status for all assets with optional filtering")
    @PreAuthorize("hasAnyRole('ADMIN', 'ENGINEER', 'OPERATOR', 'VIEWER')")
    public ResponseEntity<ApiResponse<Page<AssetHealthResponse>>> getAllAssetHealth(
            @Parameter(description = "Filter by asset type")
            @RequestParam(required = false) AssetType assetType,
            @Parameter(description = "Filter by health status")
            @RequestParam(required = false) HealthStatus status,
            @PageableDefault(size = 20) Pageable pageable) {
        
        log.debug("Getting all asset health: type={}, status={}", assetType, status);
        Page<AssetHealthResponse> health = healthMonitoringService.getAllAssetHealth(assetType, status, pageable);
        return ResponseEntity.ok(ApiResponse.success(health));
    }

    @GetMapping("/assets/{assetId}")
    @Operation(summary = "Get asset health status", 
               description = "Get the latest health status for a specific asset")
    @PreAuthorize("hasAnyRole('ADMIN', 'ENGINEER', 'OPERATOR', 'VIEWER')")
    public ResponseEntity<ApiResponse<AssetHealthResponse>> getAssetHealth(
            @PathVariable UUID assetId) {
        
        log.debug("Getting health status for asset: {}", assetId);
        AssetHealthResponse health = healthMonitoringService.getAssetHealth(assetId);
        return ResponseEntity.ok(ApiResponse.success(health));
    }

    @GetMapping("/assets/{assetId}/history")
    @Operation(summary = "Get asset health history", 
               description = "Get health status history for a specific asset")
    @PreAuthorize("hasAnyRole('ADMIN', 'ENGINEER', 'OPERATOR', 'VIEWER')")
    public ResponseEntity<ApiResponse<Page<AssetHealthResponse>>> getAssetHealthHistory(
            @PathVariable UUID assetId,
            @Parameter(description = "Start date/time")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @Parameter(description = "End date/time")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @PageableDefault(size = 50) Pageable pageable) {
        
        log.debug("Getting health history for asset {} from {} to {}", assetId, from, to);
        Page<AssetHealthResponse> history = healthMonitoringService.getAssetHealthHistory(assetId, from, to, pageable);
        return ResponseEntity.ok(ApiResponse.success(history));
    }

    @GetMapping("/corridor-summary")
    @Operation(summary = "Get corridor health summary", 
               description = "Get aggregated health summary for all assets")
    @PreAuthorize("hasAnyRole('ADMIN', 'ENGINEER', 'OPERATOR', 'VIEWER')")
    public ResponseEntity<ApiResponse<CorridorHealthSummary>> getCorridorSummary() {
        
        log.debug("Getting corridor health summary");
        CorridorHealthSummary summary = healthMonitoringService.getCorridorSummary();
        return ResponseEntity.ok(ApiResponse.success(summary));
    }

    @GetMapping("/attention-required")
    @Operation(summary = "Get assets requiring attention", 
               description = "Get assets with critical status or degrading health")
    @PreAuthorize("hasAnyRole('ADMIN', 'ENGINEER', 'OPERATOR')")
    public ResponseEntity<ApiResponse<List<AssetHealthResponse>>> getAssetsRequiringAttention() {
        
        log.debug("Getting assets requiring attention");
        List<AssetHealthResponse> assets = healthMonitoringService.getAssetsRequiringAttention();
        return ResponseEntity.ok(ApiResponse.success(assets));
    }

    // ================= Threshold Management Endpoints =================

    @GetMapping("/thresholds")
    @Operation(summary = "Get all thresholds", 
               description = "Get all health thresholds")
    @PreAuthorize("hasAnyRole('ADMIN', 'ENGINEER')")
    public ResponseEntity<ApiResponse<List<HealthThresholdResponse>>> getAllThresholds() {
        
        log.debug("Getting all health thresholds");
        List<HealthThresholdResponse> thresholds = healthMonitoringService.getAllThresholds();
        return ResponseEntity.ok(ApiResponse.success(thresholds));
    }

    @GetMapping("/thresholds/by-asset-type/{assetType}")
    @Operation(summary = "Get thresholds by asset type", 
               description = "Get health thresholds for a specific asset type")
    @PreAuthorize("hasAnyRole('ADMIN', 'ENGINEER')")
    public ResponseEntity<ApiResponse<List<HealthThresholdResponse>>> getThresholdsByAssetType(
            @PathVariable AssetType assetType) {
        
        log.debug("Getting thresholds for asset type: {}", assetType);
        List<HealthThresholdResponse> thresholds = healthMonitoringService.getThresholdsByAssetType(assetType);
        return ResponseEntity.ok(ApiResponse.success(thresholds));
    }

    @PostMapping("/thresholds")
    @Operation(summary = "Create threshold", 
               description = "Create a new health threshold")
    @PreAuthorize("hasAnyRole('ADMIN', 'ENGINEER')")
    public ResponseEntity<ApiResponse<HealthThresholdResponse>> createThreshold(
            @Valid @RequestBody HealthThresholdUpdateRequest request) {
        
        log.info("Creating new threshold: {} / {} / {}", 
                request.getAssetType(), request.getSensorType(), request.getMetricName());
        HealthThresholdResponse threshold = healthMonitoringService.createThreshold(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(threshold, "Threshold created successfully"));
    }

    @PutMapping("/thresholds/{thresholdId}")
    @Operation(summary = "Update threshold", 
               description = "Update an existing health threshold")
    @PreAuthorize("hasAnyRole('ADMIN', 'ENGINEER')")
    public ResponseEntity<ApiResponse<HealthThresholdResponse>> updateThreshold(
            @PathVariable UUID thresholdId,
            @Valid @RequestBody HealthThresholdUpdateRequest request) {
        
        log.info("Updating threshold: {}", thresholdId);
        HealthThresholdResponse threshold = healthMonitoringService.updateThreshold(thresholdId, request);
        return ResponseEntity.ok(ApiResponse.success(threshold, "Threshold updated successfully"));
    }

    @DeleteMapping("/thresholds/{thresholdId}")
    @Operation(summary = "Delete threshold", 
               description = "Delete a health threshold")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteThreshold(
            @PathVariable UUID thresholdId) {
        
        log.info("Deleting threshold: {}", thresholdId);
        healthMonitoringService.deleteThreshold(thresholdId);
        return ResponseEntity.ok(ApiResponse.success(null, "Threshold deleted successfully"));
    }
}
