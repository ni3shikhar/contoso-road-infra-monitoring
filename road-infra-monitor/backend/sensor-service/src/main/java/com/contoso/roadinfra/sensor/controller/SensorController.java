package com.contoso.roadinfra.sensor.controller;

import com.contoso.roadinfra.common.dto.ApiResponse;
import com.contoso.roadinfra.sensor.dto.*;
import com.contoso.roadinfra.sensor.security.SensorPermissionEvaluator;
import com.contoso.roadinfra.sensor.service.SensorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for sensor management operations.
 */
@RestController
@RequestMapping("/api/v1/sensors")
@RequiredArgsConstructor
@Tag(name = "Sensors", description = "Sensor management endpoints")
@SecurityRequirement(name = "bearerAuth")
public class SensorController {

    private final SensorService sensorService;
    private final SensorPermissionEvaluator permissionEvaluator;

    @GetMapping
    @PreAuthorize("@permissionEvaluator.hasPermission(authentication, 'SENSOR_READ')")
    @Operation(summary = "Get all sensors", description = "Retrieve all sensors with pagination")
    public ResponseEntity<ApiResponse<Page<SensorResponse>>> getAllSensors(
            @PageableDefault(size = 20) Pageable pageable) {
        Page<SensorResponse> sensors = sensorService.getAllSensors(pageable);
        return ResponseEntity.ok(ApiResponse.success(sensors));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@permissionEvaluator.hasPermission(authentication, 'SENSOR_READ')")
    @Operation(summary = "Get sensor by ID", description = "Retrieve a specific sensor by its ID")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Sensor found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Sensor not found")
    })
    public ResponseEntity<ApiResponse<SensorResponse>> getSensorById(
            @Parameter(description = "Sensor ID") @PathVariable UUID id) {
        SensorResponse sensor = sensorService.getSensorById(id);
        return ResponseEntity.ok(ApiResponse.success(sensor));
    }

    @GetMapping("/code/{sensorCode}")
    @PreAuthorize("@permissionEvaluator.hasPermission(authentication, 'SENSOR_READ')")
    @Operation(summary = "Get sensor by code", description = "Retrieve a specific sensor by its unique code")
    public ResponseEntity<ApiResponse<SensorResponse>> getSensorByCode(
            @Parameter(description = "Sensor code", example = "SG-BR-001") @PathVariable String sensorCode) {
        SensorResponse sensor = sensorService.getSensorByCode(sensorCode);
        return ResponseEntity.ok(ApiResponse.success(sensor));
    }

    @GetMapping("/asset/{assetId}")
    @PreAuthorize("@permissionEvaluator.hasPermission(authentication, 'SENSOR_READ')")
    @Operation(summary = "Get sensors by asset", description = "Retrieve all sensors for a specific asset")
    public ResponseEntity<ApiResponse<List<SensorResponse>>> getSensorsByAsset(
            @Parameter(description = "Asset ID") @PathVariable UUID assetId) {
        List<SensorResponse> sensors = sensorService.getSensorsByAssetId(assetId);
        return ResponseEntity.ok(ApiResponse.success(sensors));
    }

    @GetMapping("/stats/by-type")
    @PreAuthorize("@permissionEvaluator.hasPermission(authentication, 'SENSOR_READ')")
    @Operation(summary = "Get sensor counts by type", description = "Get count of sensors grouped by type")
    public ResponseEntity<ApiResponse<List<SensorCountByTypeResponse>>> getSensorCountsByType() {
        List<SensorCountByTypeResponse> stats = sensorService.getSensorCountsByType();
        return ResponseEntity.ok(ApiResponse.success(stats));
    }

    @GetMapping("/stats/by-status")
    @PreAuthorize("@permissionEvaluator.hasPermission(authentication, 'SENSOR_READ')")
    @Operation(summary = "Get sensor counts by status", description = "Get count of sensors grouped by status")
    public ResponseEntity<ApiResponse<List<SensorCountByStatusResponse>>> getSensorCountsByStatus() {
        List<SensorCountByStatusResponse> stats = sensorService.getSensorCountsByStatus();
        return ResponseEntity.ok(ApiResponse.success(stats));
    }

    @PostMapping
    @PreAuthorize("@permissionEvaluator.hasPermission(authentication, 'SENSOR_WRITE')")
    @Operation(summary = "Register sensor", description = "Register a new sensor (ADMIN, ENGINEER only)")
    public ResponseEntity<ApiResponse<SensorResponse>> createSensor(
            @Valid @RequestBody SensorCreateRequest request,
            Authentication authentication) {
        String username = permissionEvaluator.getUsernameFromAuthentication(authentication);
        SensorResponse created = sensorService.createSensor(request, username);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(created, "Sensor registered successfully"));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@permissionEvaluator.hasPermission(authentication, 'SENSOR_WRITE')")
    @Operation(summary = "Update sensor", description = "Update an existing sensor (ADMIN, ENGINEER only)")
    public ResponseEntity<ApiResponse<SensorResponse>> updateSensor(
            @PathVariable UUID id,
            @Valid @RequestBody SensorUpdateRequest request,
            Authentication authentication) {
        String username = permissionEvaluator.getUsernameFromAuthentication(authentication);
        SensorResponse updated = sensorService.updateSensor(id, request, username);
        return ResponseEntity.ok(ApiResponse.success(updated, "Sensor updated successfully"));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("@permissionEvaluator.hasPermission(authentication, 'SENSOR_CONFIGURE')")
    @Operation(summary = "Update sensor status", description = "Update sensor status (ADMIN, ENGINEER only)")
    public ResponseEntity<ApiResponse<SensorResponse>> updateSensorStatus(
            @PathVariable UUID id,
            @Valid @RequestBody SensorStatusUpdateRequest request,
            Authentication authentication) {
        String username = permissionEvaluator.getUsernameFromAuthentication(authentication);
        SensorResponse updated = sensorService.updateSensorStatus(id, request, username);
        return ResponseEntity.ok(ApiResponse.success(updated, "Sensor status updated successfully"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@permissionEvaluator.hasPermission(authentication, 'SENSOR_DELETE')")
    @Operation(summary = "Decommission sensor", description = "Decommission a sensor (ADMIN only)")
    public ResponseEntity<ApiResponse<Void>> decommissionSensor(
            @PathVariable UUID id,
            Authentication authentication) {
        String username = permissionEvaluator.getUsernameFromAuthentication(authentication);
        sensorService.decommissionSensor(id, username);
        return ResponseEntity.ok(ApiResponse.success(null, "Sensor decommissioned successfully"));
    }
}
