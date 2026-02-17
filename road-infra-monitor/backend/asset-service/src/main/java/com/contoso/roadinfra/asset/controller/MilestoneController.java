package com.contoso.roadinfra.asset.controller;

import com.contoso.roadinfra.asset.dto.*;
import com.contoso.roadinfra.asset.service.MilestoneService;
import com.contoso.roadinfra.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for managing construction milestones.
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Milestones", description = "Construction milestone management endpoints")
public class MilestoneController {

    private final MilestoneService milestoneService;

    @PostMapping("/assets/{assetId}/milestones")
    @PreAuthorize("hasAnyRole('ADMIN', 'ENGINEER')")
    @Operation(summary = "Create milestone", description = "Create a new milestone for an asset")
    public ResponseEntity<ApiResponse<MilestoneResponse>> createMilestone(
            @Parameter(description = "Asset ID") @PathVariable UUID assetId,
            @Valid @RequestBody MilestoneCreateRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        MilestoneResponse created = milestoneService.createMilestone(assetId, request, userDetails.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(created, "Milestone created successfully"));
    }

    @GetMapping("/assets/{assetId}/milestones")
    @PreAuthorize("hasAnyRole('ADMIN', 'ENGINEER', 'OPERATOR', 'VIEWER')")
    @Operation(summary = "Get milestones for asset", description = "Retrieve milestones for an asset")
    public ResponseEntity<ApiResponse<Page<MilestoneResponse>>> getMilestonesForAsset(
            @Parameter(description = "Asset ID") @PathVariable UUID assetId,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<MilestoneResponse> milestones = milestoneService.getMilestonesForAsset(assetId, pageable);
        return ResponseEntity.ok(ApiResponse.success(milestones));
    }

    @GetMapping("/milestones/{milestoneId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'ENGINEER', 'OPERATOR', 'VIEWER')")
    @Operation(summary = "Get milestone by ID", description = "Retrieve a specific milestone")
    public ResponseEntity<ApiResponse<MilestoneResponse>> getMilestoneById(
            @Parameter(description = "Milestone ID") @PathVariable UUID milestoneId) {
        MilestoneResponse milestone = milestoneService.getMilestoneById(milestoneId);
        return ResponseEntity.ok(ApiResponse.success(milestone));
    }

    @PutMapping("/milestones/{milestoneId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'ENGINEER')")
    @Operation(summary = "Update milestone", description = "Update a milestone")
    public ResponseEntity<ApiResponse<MilestoneResponse>> updateMilestone(
            @Parameter(description = "Milestone ID") @PathVariable UUID milestoneId,
            @Valid @RequestBody MilestoneUpdateRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        MilestoneResponse updated = milestoneService.updateMilestone(milestoneId, request, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success(updated, "Milestone updated successfully"));
    }

    @PatchMapping("/milestones/{milestoneId}/start")
    @PreAuthorize("hasAnyRole('ADMIN', 'ENGINEER', 'OPERATOR')")
    @Operation(summary = "Start milestone", description = "Mark a milestone as in progress")
    public ResponseEntity<ApiResponse<MilestoneResponse>> startMilestone(
            @Parameter(description = "Milestone ID") @PathVariable UUID milestoneId,
            @AuthenticationPrincipal UserDetails userDetails) {
        MilestoneResponse started = milestoneService.startMilestone(milestoneId, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success(started, "Milestone started"));
    }

    @PatchMapping("/milestones/{milestoneId}/complete")
    @PreAuthorize("hasAnyRole('ADMIN', 'ENGINEER', 'OPERATOR')")
    @Operation(summary = "Complete milestone", description = "Mark a milestone as completed")
    public ResponseEntity<ApiResponse<MilestoneResponse>> completeMilestone(
            @Parameter(description = "Milestone ID") @PathVariable UUID milestoneId,
            @Valid @RequestBody MilestoneCompleteRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        MilestoneResponse completed = milestoneService.completeMilestone(milestoneId, request, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success(completed, "Milestone completed"));
    }

    @PatchMapping("/milestones/{milestoneId}/delay")
    @PreAuthorize("hasAnyRole('ADMIN', 'ENGINEER')")
    @Operation(summary = "Mark milestone delayed", description = "Mark a milestone as delayed with reason")
    public ResponseEntity<ApiResponse<MilestoneResponse>> markDelayed(
            @Parameter(description = "Milestone ID") @PathVariable UUID milestoneId,
            @RequestParam String reason,
            @AuthenticationPrincipal UserDetails userDetails) {
        MilestoneResponse delayed = milestoneService.markDelayed(milestoneId, reason, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success(delayed, "Milestone marked as delayed"));
    }

    @DeleteMapping("/milestones/{milestoneId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete milestone", description = "Delete a milestone (admin only)")
    public ResponseEntity<ApiResponse<Void>> deleteMilestone(
            @Parameter(description = "Milestone ID") @PathVariable UUID milestoneId,
            @AuthenticationPrincipal UserDetails userDetails) {
        milestoneService.deleteMilestone(milestoneId, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success(null, "Milestone deleted successfully"));
    }

    @GetMapping("/milestones/delayed")
    @PreAuthorize("hasAnyRole('ADMIN', 'ENGINEER', 'OPERATOR')")
    @Operation(summary = "Get delayed milestones", description = "Retrieve all delayed milestones")
    public ResponseEntity<ApiResponse<List<MilestoneService.DelayedMilestoneResponse>>> getDelayedMilestones() {
        List<MilestoneService.DelayedMilestoneResponse> delayed = milestoneService.getDelayedMilestones();
        return ResponseEntity.ok(ApiResponse.success(delayed));
    }

    @GetMapping("/milestones/gantt/{assetId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'ENGINEER', 'OPERATOR', 'VIEWER')")
    @Operation(summary = "Get Gantt chart data", description = "Retrieve Gantt chart data for an asset's milestones")
    public ResponseEntity<ApiResponse<GanttChartResponse>> getGanttChart(
            @Parameter(description = "Asset ID") @PathVariable UUID assetId) {
        GanttChartResponse gantt = milestoneService.getGanttChart(assetId);
        return ResponseEntity.ok(ApiResponse.success(gantt));
    }
}
