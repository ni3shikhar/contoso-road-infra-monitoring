package com.contoso.roadinfra.sensor.controller;

import com.contoso.roadinfra.common.dto.ApiResponse;
import com.contoso.roadinfra.sensor.dto.*;
import com.contoso.roadinfra.sensor.service.SensorReadingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * REST controller for sensor reading operations.
 */
@RestController
@RequestMapping("/api/v1/sensors")
@RequiredArgsConstructor
@Tag(name = "Sensor Readings", description = "Sensor reading and telemetry endpoints")
@SecurityRequirement(name = "bearerAuth")
public class SensorReadingController {

    private final SensorReadingService readingService;

    @PostMapping("/{id}/readings")
    @PreAuthorize("@permissionEvaluator.hasPermission(authentication, 'SENSOR_WRITE')")
    @Operation(summary = "Ingest sensor reading",
            description = "Ingest a new telemetry reading for a sensor (ADMIN, ENGINEER, or system service)")
    public ResponseEntity<ApiResponse<SensorReadingResponse>> ingestReading(
            @Parameter(description = "Sensor ID") @PathVariable UUID id,
            @Valid @RequestBody SensorReadingRequest request) {
        SensorReadingResponse response = readingService.ingestReading(id, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Reading ingested successfully"));
    }

    @PostMapping("/readings/batch")
    @PreAuthorize("@permissionEvaluator.hasPermission(authentication, 'SENSOR_WRITE')")
    @Operation(summary = "Batch ingest readings",
            description = "Batch ingest multiple sensor readings (ADMIN, ENGINEER, or system service)")
    public ResponseEntity<ApiResponse<List<SensorReadingResponse>>> batchIngestReadings(
            @Valid @RequestBody BatchReadingRequest request) {
        List<SensorReadingResponse> responses = readingService.batchIngestReadings(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(responses,
                        String.format("%d of %d readings ingested successfully",
                                responses.size(), request.getReadings().size())));
    }

    @GetMapping("/{id}/readings")
    @PreAuthorize("@permissionEvaluator.hasPermission(authentication, 'SENSOR_READ')")
    @Operation(summary = "Get sensor readings",
            description = "Get readings for a sensor with pagination")
    public ResponseEntity<ApiResponse<Page<SensorReadingResponse>>> getReadings(
            @Parameter(description = "Sensor ID") @PathVariable UUID id,
            @PageableDefault(size = 50, sort = "timestamp") Pageable pageable) {
        Page<SensorReadingResponse> readings = readingService.getReadings(id, pageable);
        return ResponseEntity.ok(ApiResponse.success(readings));
    }

    @GetMapping("/{id}/readings/range")
    @PreAuthorize("@permissionEvaluator.hasPermission(authentication, 'SENSOR_READ')")
    @Operation(summary = "Get sensor readings in time range",
            description = "Get readings for a sensor within a specific time range")
    public ResponseEntity<ApiResponse<List<SensorReadingResponse>>> getReadingsInRange(
            @Parameter(description = "Sensor ID") @PathVariable UUID id,
            @Parameter(description = "Start time (ISO 8601)")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant start,
            @Parameter(description = "End time (ISO 8601)")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant end) {
        List<SensorReadingResponse> readings = readingService.getReadingsInRange(id, start, end);
        return ResponseEntity.ok(ApiResponse.success(readings));
    }

    @GetMapping("/{id}/readings/latest")
    @PreAuthorize("@permissionEvaluator.hasPermission(authentication, 'SENSOR_READ')")
    @Operation(summary = "Get latest reading",
            description = "Get the most recent reading for a sensor")
    public ResponseEntity<ApiResponse<SensorReadingResponse>> getLatestReading(
            @Parameter(description = "Sensor ID") @PathVariable UUID id) {
        SensorReadingResponse reading = readingService.getLatestReading(id);
        if (reading == null) {
            return ResponseEntity.ok(ApiResponse.success(null, "No readings found for this sensor"));
        }
        return ResponseEntity.ok(ApiResponse.success(reading));
    }

    @GetMapping("/{id}/readings/stats")
    @PreAuthorize("@permissionEvaluator.hasPermission(authentication, 'SENSOR_READ')")
    @Operation(summary = "Get reading statistics",
            description = "Get aggregated statistics for sensor readings in a time range")
    public ResponseEntity<ApiResponse<SensorReadingStatsResponse>> getReadingStats(
            @Parameter(description = "Sensor ID") @PathVariable UUID id,
            @Parameter(description = "Start time (ISO 8601, defaults to 24h ago)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant start,
            @Parameter(description = "End time (ISO 8601, defaults to now)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant end) {
        SensorReadingStatsResponse stats = readingService.getReadingStats(id, start, end);
        return ResponseEntity.ok(ApiResponse.success(stats));
    }
}
