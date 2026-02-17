package com.contoso.roadinfra.monitoring.controller;

import com.contoso.roadinfra.common.constants.HealthStatus;
import com.contoso.roadinfra.common.dto.ApiResponse;
import com.contoso.roadinfra.common.dto.HealthStatusDTO;
import com.contoso.roadinfra.monitoring.service.MonitoringService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/monitoring")
@RequiredArgsConstructor
@Tag(name = "Monitoring", description = "Real-time health monitoring endpoints")
public class MonitoringController {

    private final MonitoringService monitoringService;

    @GetMapping("/health-status/{assetId}")
    @Operation(summary = "Get latest health status", description = "Get the latest health status for an asset")
    public ResponseEntity<ApiResponse<HealthStatusDTO>> getLatestHealthStatus(
            @PathVariable UUID assetId) {
        HealthStatusDTO status = monitoringService.getLatestHealthStatus(assetId);
        return ResponseEntity.ok(ApiResponse.success(status));
    }

    @GetMapping("/health-status/{assetId}/history")
    @Operation(summary = "Get health history", description = "Get health status history for an asset")
    public ResponseEntity<ApiResponse<Page<HealthStatusDTO>>> getHealthHistory(
            @PathVariable UUID assetId,
            @PageableDefault(size = 50) Pageable pageable) {
        Page<HealthStatusDTO> history = monitoringService.getHealthHistory(assetId, pageable);
        return ResponseEntity.ok(ApiResponse.success(history));
    }

    @GetMapping("/status-changes")
    @Operation(summary = "Get recent status changes", description = "Get status changes in last 24 hours")
    public ResponseEntity<ApiResponse<List<HealthStatusDTO>>> getRecentStatusChanges() {
        List<HealthStatusDTO> changes = monitoringService.getRecentStatusChanges();
        return ResponseEntity.ok(ApiResponse.success(changes));
    }

    @PostMapping("/health-check")
    @Operation(summary = "Record health check", description = "Record a health check for an asset")
    public ResponseEntity<ApiResponse<HealthStatusDTO>> recordHealthCheck(
            @RequestParam UUID assetId,
            @RequestParam String assetName,
            @RequestParam HealthStatus status,
            @RequestParam(required = false) Integer healthScore,
            @RequestBody(required = false) Map<String, Double> metrics) {
        HealthStatusDTO recorded = monitoringService.recordHealthCheck(assetId, assetName, status, healthScore, metrics);
        return ResponseEntity.ok(ApiResponse.success(recorded, "Health check recorded"));
    }
}
