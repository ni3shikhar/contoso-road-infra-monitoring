package com.contoso.roadinfra.alert.controller;

import com.contoso.roadinfra.alert.service.AlertService;
import com.contoso.roadinfra.alert.service.EscalationService;
import com.contoso.roadinfra.common.dto.AlertDTO;
import com.contoso.roadinfra.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
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
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/alerts")
@RequiredArgsConstructor
@Tag(name = "Alerts", description = "Alert management endpoints")
@SecurityRequirement(name = "bearerAuth")
public class AlertController {

    private final AlertService alertService;
    private final EscalationService escalationService;

    @GetMapping
    @Operation(summary = "Get all alerts", description = "Retrieve all alerts with pagination")
    @PreAuthorize("hasAnyRole('ADMIN', 'ENGINEER', 'OPERATOR', 'VIEWER')")
    public ResponseEntity<ApiResponse<Page<AlertDTO>>> getAllAlerts(
            @PageableDefault(size = 20) Pageable pageable) {
        Page<AlertDTO> alerts = alertService.getAllAlerts(pageable);
        return ResponseEntity.ok(ApiResponse.success(alerts));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get alert by ID")
    @PreAuthorize("hasAnyRole('ADMIN', 'ENGINEER', 'OPERATOR', 'VIEWER')")
    public ResponseEntity<ApiResponse<AlertDTO>> getAlertById(@PathVariable UUID id) {
        AlertDTO alert = alertService.getAlertById(id);
        return ResponseEntity.ok(ApiResponse.success(alert));
    }

    @GetMapping("/active")
    @Operation(summary = "Get active alerts", description = "Get all unresolved alerts")
    @PreAuthorize("hasAnyRole('ADMIN', 'ENGINEER', 'OPERATOR', 'VIEWER')")
    public ResponseEntity<ApiResponse<List<AlertDTO>>> getActiveAlerts() {
        List<AlertDTO> alerts = alertService.getActiveAlerts();
        return ResponseEntity.ok(ApiResponse.success(alerts));
    }

    @GetMapping("/asset/{assetId}")
    @Operation(summary = "Get alerts by asset")
    @PreAuthorize("hasAnyRole('ADMIN', 'ENGINEER', 'OPERATOR', 'VIEWER')")
    public ResponseEntity<ApiResponse<List<AlertDTO>>> getAlertsByAsset(@PathVariable UUID assetId) {
        List<AlertDTO> alerts = alertService.getAlertsByAsset(assetId);
        return ResponseEntity.ok(ApiResponse.success(alerts));
    }

    @GetMapping("/critical/unacknowledged")
    @Operation(summary = "Get unacknowledged critical alerts")
    @PreAuthorize("hasAnyRole('ADMIN', 'ENGINEER', 'OPERATOR')")
    public ResponseEntity<ApiResponse<List<AlertDTO>>> getUnacknowledgedCriticalAlerts() {
        List<AlertDTO> alerts = alertService.getUnacknowledgedCriticalAlerts();
        return ResponseEntity.ok(ApiResponse.success(alerts));
    }

    @GetMapping("/statistics")
    @Operation(summary = "Get alert statistics")
    @PreAuthorize("hasAnyRole('ADMIN', 'ENGINEER', 'OPERATOR', 'VIEWER')")
    public ResponseEntity<ApiResponse<Map<String, Long>>> getAlertStatistics() {
        Map<String, Long> stats = alertService.getAlertStatistics();
        return ResponseEntity.ok(ApiResponse.success(stats));
    }

    @GetMapping("/escalation-stats")
    @Operation(summary = "Get escalation statistics")
    @PreAuthorize("hasAnyRole('ADMIN', 'ENGINEER')")
    public ResponseEntity<ApiResponse<EscalationService.EscalationStats>> getEscalationStats() {
        EscalationService.EscalationStats stats = escalationService.getEscalationStats();
        return ResponseEntity.ok(ApiResponse.success(stats));
    }

    @PostMapping
    @Operation(summary = "Create alert")
    @PreAuthorize("hasAnyRole('ADMIN', 'ENGINEER', 'OPERATOR')")
    public ResponseEntity<ApiResponse<AlertDTO>> createAlert(@Valid @RequestBody AlertDTO alertDTO) {
        AlertDTO created = alertService.createAlert(alertDTO);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(created, "Alert created"));
    }

    @PostMapping("/{id}/acknowledge")
    @Operation(summary = "Acknowledge alert")
    @PreAuthorize("hasAnyRole('ADMIN', 'ENGINEER', 'OPERATOR')")
    public ResponseEntity<ApiResponse<AlertDTO>> acknowledgeAlert(
            @PathVariable UUID id,
            @RequestParam UUID userId,
            @RequestParam String userName) {
        AlertDTO acknowledged = alertService.acknowledgeAlert(id, userId, userName);
        return ResponseEntity.ok(ApiResponse.success(acknowledged, "Alert acknowledged"));
    }

    @PostMapping("/{id}/resolve")
    @Operation(summary = "Resolve alert")
    @PreAuthorize("hasAnyRole('ADMIN', 'ENGINEER', 'OPERATOR')")
    public ResponseEntity<ApiResponse<AlertDTO>> resolveAlert(
            @PathVariable UUID id,
            @RequestParam UUID userId,
            @RequestParam String userName,
            @RequestParam(required = false) String resolutionNotes) {
        AlertDTO resolved = alertService.resolveAlert(id, userId, userName, resolutionNotes);
        return ResponseEntity.ok(ApiResponse.success(resolved, "Alert resolved"));
    }

    @PostMapping("/{id}/escalate")
    @Operation(summary = "Manually escalate alert")
    @PreAuthorize("hasAnyRole('ADMIN', 'ENGINEER')")
    public ResponseEntity<ApiResponse<Void>> escalateAlert(@PathVariable UUID id) {
        escalationService.escalateAlert(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Alert escalated"));
    }
}
