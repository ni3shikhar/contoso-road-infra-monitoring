package com.contoso.roadinfra.asset.controller;

import com.contoso.roadinfra.asset.dto.InspectionCreateRequest;
import com.contoso.roadinfra.asset.dto.InspectionResponse;
import com.contoso.roadinfra.asset.service.InspectionService;
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
 * REST controller for managing asset inspections.
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Inspections", description = "Asset inspection management endpoints")
public class InspectionController {

    private final InspectionService inspectionService;

    @PostMapping("/assets/{assetId}/inspections")
    @PreAuthorize("hasAnyRole('ADMIN', 'ENGINEER')")
    @Operation(summary = "Create inspection", description = "Record a new inspection for an asset")
    public ResponseEntity<ApiResponse<InspectionResponse>> createInspection(
            @Parameter(description = "Asset ID") @PathVariable UUID assetId,
            @Valid @RequestBody InspectionCreateRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        InspectionResponse created = inspectionService.createInspection(assetId, request, userDetails.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(created, "Inspection recorded successfully"));
    }

    @GetMapping("/assets/{assetId}/inspections")
    @PreAuthorize("hasAnyRole('ADMIN', 'ENGINEER', 'OPERATOR', 'VIEWER')")
    @Operation(summary = "Get inspections for asset", description = "Retrieve inspection history for an asset")
    public ResponseEntity<ApiResponse<Page<InspectionResponse>>> getInspectionsForAsset(
            @Parameter(description = "Asset ID") @PathVariable UUID assetId,
            @PageableDefault(size = 10) Pageable pageable) {
        Page<InspectionResponse> inspections = inspectionService.getInspectionsForAsset(assetId, pageable);
        return ResponseEntity.ok(ApiResponse.success(inspections));
    }

    @GetMapping("/inspections/{inspectionId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'ENGINEER', 'OPERATOR', 'VIEWER')")
    @Operation(summary = "Get inspection by ID", description = "Retrieve a specific inspection")
    public ResponseEntity<ApiResponse<InspectionResponse>> getInspectionById(
            @Parameter(description = "Inspection ID") @PathVariable UUID inspectionId) {
        InspectionResponse inspection = inspectionService.getInspectionById(inspectionId);
        return ResponseEntity.ok(ApiResponse.success(inspection));
    }

    @GetMapping("/inspections/overdue")
    @PreAuthorize("hasAnyRole('ADMIN', 'ENGINEER', 'OPERATOR')")
    @Operation(summary = "Get overdue inspections", description = "Retrieve assets with overdue inspections")
    public ResponseEntity<ApiResponse<List<InspectionService.AssetInspectionOverdue>>> getOverdueInspections() {
        List<InspectionService.AssetInspectionOverdue> overdue = inspectionService.getOverdueInspections();
        return ResponseEntity.ok(ApiResponse.success(overdue));
    }

    @GetMapping("/assets/{assetId}/inspections/statistics")
    @PreAuthorize("hasAnyRole('ADMIN', 'ENGINEER', 'OPERATOR', 'VIEWER')")
    @Operation(summary = "Get inspection statistics", description = "Get inspection statistics for an asset")
    public ResponseEntity<ApiResponse<InspectionService.InspectionStatistics>> getInspectionStatistics(
            @Parameter(description = "Asset ID") @PathVariable UUID assetId) {
        InspectionService.InspectionStatistics stats = inspectionService.getInspectionStatistics(assetId);
        return ResponseEntity.ok(ApiResponse.success(stats));
    }
}
