package com.contoso.roadinfra.asset.dto;

import com.contoso.roadinfra.asset.constants.ConstructionStatus;
import com.contoso.roadinfra.common.constants.AssetType;
import com.contoso.roadinfra.common.constants.HealthStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to create a new asset")
public class AssetCreateRequest {

    @NotBlank(message = "Asset code is required")
    @Size(max = 50, message = "Asset code must be at most 50 characters")
    @Pattern(regexp = "^[A-Z]{2,3}-[A-Z]{2,3}-\\d{3}$", message = "Asset code must follow pattern like RD-SEC-001")
    @Schema(description = "Unique asset code", example = "RD-SEC-001")
    private String assetCode;

    @NotBlank(message = "Name is required")
    @Size(max = 200, message = "Name must be at most 200 characters")
    @Schema(description = "Asset name", example = "Main Corridor Road Section 1")
    private String name;

    @NotNull(message = "Asset type is required")
    @Schema(description = "Type of the asset")
    private AssetType assetType;

    @Size(max = 4000, message = "Description must be at most 4000 characters")
    @Schema(description = "Detailed description of the asset")
    private String description;

    @NotNull(message = "Start chainage is required")
    @Min(value = 0, message = "Start chainage must be non-negative")
    @Schema(description = "Distance in meters from corridor start", example = "0.0")
    private Double startChainage;

    @NotNull(message = "End chainage is required")
    @Min(value = 0, message = "End chainage must be non-negative")
    @Schema(description = "Distance in meters from corridor start", example = "450.0")
    private Double endChainage;

    @Schema(description = "Length in meters (calculated if not provided)")
    private Double length;

    @NotNull(message = "Start latitude is required")
    @DecimalMin(value = "-90.0", message = "Latitude must be between -90 and 90")
    @DecimalMax(value = "90.0", message = "Latitude must be between -90 and 90")
    @Schema(description = "Start point latitude", example = "40.7128")
    private Double startLatitude;

    @NotNull(message = "Start longitude is required")
    @DecimalMin(value = "-180.0", message = "Longitude must be between -180 and 180")
    @DecimalMax(value = "180.0", message = "Longitude must be between -180 and 180")
    @Schema(description = "Start point longitude", example = "-74.0060")
    private Double startLongitude;

    @Schema(description = "End point latitude")
    private Double endLatitude;

    @Schema(description = "End point longitude")
    private Double endLongitude;

    @Schema(description = "Construction start date")
    private LocalDate constructionStartDate;

    @Schema(description = "Expected completion date")
    private LocalDate expectedCompletionDate;

    @Schema(description = "Initial construction status")
    private ConstructionStatus status;

    @Schema(description = "Initial health status")
    private HealthStatus healthStatus;

    @Min(value = 1, message = "Design life must be at least 1 year")
    @Max(value = 200, message = "Design life cannot exceed 200 years")
    @Schema(description = "Design life in years", example = "50")
    private Integer designLifeYears;

    @Schema(description = "Parent asset ID for hierarchical assets")
    private UUID parentAssetId;

    @Schema(description = "JSON metadata for flexible attributes", example = "{\"surfaceType\": \"asphalt\"}")
    private String metadata;
}
