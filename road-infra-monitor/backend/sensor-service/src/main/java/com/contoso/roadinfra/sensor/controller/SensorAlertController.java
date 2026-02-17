package com.contoso.roadinfra.sensor.controller;

import com.contoso.roadinfra.common.constants.AlertSeverity;
import com.contoso.roadinfra.common.constants.SensorAlertType;
import com.contoso.roadinfra.common.dto.ApiResponse;
import com.contoso.roadinfra.sensor.dto.SensorAlertResponse;
import com.contoso.roadinfra.sensor.security.SensorPermissionEvaluator;
import com.contoso.roadinfra.sensor.service.SensorAlertService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * REST controller for sensor alert operations.
 */
@RestController
@RequestMapping("/api/v1/sensors/alerts")
@RequiredArgsConstructor
@Tag(name = "Sensor Alerts", description = "Sensor alert management endpoints")
@SecurityRequirement(name = "bearerAuth")
public class SensorAlertController {

    private final SensorAlertService alertService;
    private final SensorPermissionEvaluator permissionEvaluator;

    @GetMapping
    @PreAuthorize("@permissionEvaluator.hasPermission(authentication, 'ALERT_READ')")
    @Operation(summary = "Get all alerts",
            description = "Get all sensor alerts with pagination")
    public ResponseEntity<ApiResponse<Page<SensorAlertResponse>>> getAllAlerts(
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        Page<SensorAlertResponse> alerts = alertService.getAllAlerts(pageable);
        return ResponseEntity.ok(ApiResponse.success(alerts));
    }

    @GetMapping("/unacknowledged")
    @PreAuthorize("@permissionEvaluator.hasPermission(authentication, 'ALERT_READ')")
    @Operation(summary = "Get unacknowledged alerts",
            description = "Get all unacknowledged sensor alerts")
    public ResponseEntity<ApiResponse<Page<SensorAlertResponse>>> getUnacknowledgedAlerts(
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        Page<SensorAlertResponse> alerts = alertService.getUnacknowledgedAlerts(pageable);
        return ResponseEntity.ok(ApiResponse.success(alerts));
    }

    @GetMapping("/severity/{severity}")
    @PreAuthorize("@permissionEvaluator.hasPermission(authentication, 'ALERT_READ')")
    @Operation(summary = "Get alerts by severity",
            description = "Get sensor alerts filtered by severity level")
    public ResponseEntity<ApiResponse<Page<SensorAlertResponse>>> getAlertsBySeverity(
            @Parameter(description = "Alert severity") @PathVariable AlertSeverity severity,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        Page<SensorAlertResponse> alerts = alertService.getAlertsBySeverity(severity, pageable);
        return ResponseEntity.ok(ApiResponse.success(alerts));
    }

    @GetMapping("/type/{alertType}")
    @PreAuthorize("@permissionEvaluator.hasPermission(authentication, 'ALERT_READ')")
    @Operation(summary = "Get alerts by type",
            description = "Get sensor alerts filtered by alert type")
    public ResponseEntity<ApiResponse<Page<SensorAlertResponse>>> getAlertsByType(
            @Parameter(description = "Alert type") @PathVariable SensorAlertType alertType,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        Page<SensorAlertResponse> alerts = alertService.getAlertsByType(alertType, pageable);
        return ResponseEntity.ok(ApiResponse.success(alerts));
    }

    @GetMapping("/count/unacknowledged")
    @PreAuthorize("@permissionEvaluator.hasPermission(authentication, 'ALERT_READ')")
    @Operation(summary = "Get unacknowledged alert count",
            description = "Get the count of unacknowledged alerts")
    public ResponseEntity<ApiResponse<Long>> getUnacknowledgedAlertCount() {
        Long count = alertService.getUnacknowledgedAlertCount();
        return ResponseEntity.ok(ApiResponse.success(count));
    }

    @PatchMapping("/{id}/acknowledge")
    @PreAuthorize("@permissionEvaluator.hasPermission(authentication, 'ALERT_ACKNOWLEDGE')")
    @Operation(summary = "Acknowledge alert",
            description = "Acknowledge a sensor alert (ADMIN, ENGINEER, OPERATOR only)")
    public ResponseEntity<ApiResponse<SensorAlertResponse>> acknowledgeAlert(
            @Parameter(description = "Alert ID") @PathVariable UUID id,
            Authentication authentication) {
        String username = permissionEvaluator.getUsernameFromAuthentication(authentication);
        SensorAlertResponse response = alertService.acknowledgeAlert(id, username);
        return ResponseEntity.ok(ApiResponse.success(response, "Alert acknowledged successfully"));
    }
}
