package com.contoso.roadinfra.asset.controller;

import com.contoso.roadinfra.asset.dto.*;
import com.contoso.roadinfra.asset.service.AssetService;
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
 * REST controller for managing infrastructure assets.
 */
@RestController
@RequestMapping("/api/v1/assets")
@RequiredArgsConstructor
@Tag(name = "Assets", description = "Asset lifecycle management endpoints")
public class AssetController {

    private final AssetService assetService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'ENGINEER', 'OPERATOR', 'VIEWER')")
    @Operation(summary = "Get all assets", description = "Retrieve all assets with pagination")
    public ResponseEntity<ApiResponse<Page<AssetResponse>>> getAllAssets(
            @PageableDefault(size = 20) Pageable pageable) {
        Page<AssetResponse> assets = assetService.getAllAssets(pageable);
        return ResponseEntity.ok(ApiResponse.success(assets));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'ENGINEER', 'OPERATOR', 'VIEWER')")
    @Operation(summary = "Get asset by ID", description = "Retrieve a specific asset by its ID with children and sensor count")
    public ResponseEntity<ApiResponse<AssetResponse>> getAssetById(
            @Parameter(description = "Asset ID") @PathVariable UUID id) {
        AssetResponse asset = assetService.getAssetById(id);
        return ResponseEntity.ok(ApiResponse.success(asset));
    }

    @GetMapping("/code/{assetCode}")
    @PreAuthorize("hasAnyRole('ADMIN', 'ENGINEER', 'OPERATOR', 'VIEWER')")
    @Operation(summary = "Get asset by code", description = "Retrieve a specific asset by its unique code")
    public ResponseEntity<ApiResponse<AssetResponse>> getAssetByCode(
            @Parameter(description = "Asset code") @PathVariable String assetCode) {
        AssetResponse asset = assetService.getAssetByCode(assetCode);
        return ResponseEntity.ok(ApiResponse.success(asset));
    }

    @GetMapping("/{id}/children")
    @PreAuthorize("hasAnyRole('ADMIN', 'ENGINEER', 'OPERATOR', 'VIEWER')")
    @Operation(summary = "Get child assets", description = "Retrieve all child assets of a parent asset")
    public ResponseEntity<ApiResponse<List<AssetResponse>>> getChildAssets(
            @Parameter(description = "Parent asset ID") @PathVariable UUID id) {
        List<AssetResponse> children = assetService.getChildAssets(id);
        return ResponseEntity.ok(ApiResponse.success(children));
    }

    @GetMapping("/corridor/summary")
    @PreAuthorize("hasAnyRole('ADMIN', 'ENGINEER', 'OPERATOR', 'VIEWER')")
    @Operation(summary = "Get corridor summary", description = "Retrieve summary statistics for the entire corridor")
    public ResponseEntity<ApiResponse<CorridorSummaryResponse>> getCorridorSummary() {
        CorridorSummaryResponse summary = assetService.getCorridorSummary();
        return ResponseEntity.ok(ApiResponse.success(summary));
    }

    @GetMapping("/geojson")
    @PreAuthorize("hasAnyRole('ADMIN', 'ENGINEER', 'OPERATOR', 'VIEWER')")
    @Operation(summary = "Get GeoJSON features", description = "Retrieve all assets as GeoJSON feature collection")
    public ResponseEntity<GeoJsonFeatureCollection> getGeoJsonFeatures() {
        GeoJsonFeatureCollection geoJson = assetService.getGeoJsonFeatures();
        return ResponseEntity.ok(geoJson);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'ENGINEER')")
    @Operation(summary = "Create asset", description = "Create a new infrastructure asset")
    public ResponseEntity<ApiResponse<AssetResponse>> createAsset(
            @Valid @RequestBody AssetCreateRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        AssetResponse created = assetService.createAsset(request, userDetails.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(created, "Asset created successfully"));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'ENGINEER')")
    @Operation(summary = "Update asset", description = "Update an existing asset")
    public ResponseEntity<ApiResponse<AssetResponse>> updateAsset(
            @PathVariable UUID id,
            @Valid @RequestBody AssetUpdateRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        AssetResponse updated = assetService.updateAsset(id, request, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success(updated, "Asset updated successfully"));
    }

    @PatchMapping("/{id}/progress")
    @PreAuthorize("hasAnyRole('ADMIN', 'ENGINEER', 'OPERATOR')")
    @Operation(summary = "Update asset progress", description = "Update construction progress percentage")
    public ResponseEntity<ApiResponse<AssetResponse>> updateProgress(
            @PathVariable UUID id,
            @Valid @RequestBody AssetProgressUpdateRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        AssetResponse updated = assetService.updateProgress(id, request, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success(updated, "Progress updated successfully"));
    }

    @PatchMapping("/{id}/health")
    @PreAuthorize("hasAnyRole('ADMIN', 'ENGINEER', 'OPERATOR')")
    @Operation(summary = "Update asset health", description = "Update asset health status")
    public ResponseEntity<ApiResponse<AssetResponse>> updateHealthStatus(
            @PathVariable UUID id,
            @Valid @RequestBody AssetHealthUpdateRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        AssetResponse updated = assetService.updateHealthStatus(id, request, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success(updated, "Health status updated"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete asset", description = "Delete an asset (admin only)")
    public ResponseEntity<ApiResponse<Void>> deleteAsset(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails userDetails) {
        assetService.deleteAsset(id, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success(null, "Asset deleted successfully"));
    }

    @PostMapping("/{id}/recalculate-completion")
    @PreAuthorize("hasAnyRole('ADMIN', 'ENGINEER')")
    @Operation(summary = "Recalculate completion", description = "Recalculate asset completion percentage from milestones")
    public ResponseEntity<ApiResponse<Double>> recalculateCompletion(@PathVariable UUID id) {
        Double newPercentage = assetService.recalculateCompletionFromMilestones(id);
        return ResponseEntity.ok(ApiResponse.success(newPercentage, "Completion percentage recalculated"));
    }
}
