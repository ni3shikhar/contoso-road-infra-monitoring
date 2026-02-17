package com.contoso.roadinfra.asset.dto;

import com.contoso.roadinfra.asset.constants.ConstructionStatus;
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
@Schema(description = "Request to update an existing asset")
public class AssetUpdateRequest {

    @Size(max = 200, message = "Name must be at most 200 characters")
    @Schema(description = "Asset name")
    private String name;

    @Size(max = 4000, message = "Description must be at most 4000 characters")
    @Schema(description = "Detailed description of the asset")
    private String description;

    @Min(value = 0, message = "Start chainage must be non-negative")
    @Schema(description = "Distance in meters from corridor start")
    private Double startChainage;

    @Min(value = 0, message = "End chainage must be non-negative")
    @Schema(description = "Distance in meters from corridor start")
    private Double endChainage;

    @Schema(description = "Length in meters")
    private Double length;

    @DecimalMin(value = "-90.0", message = "Latitude must be between -90 and 90")
    @DecimalMax(value = "90.0", message = "Latitude must be between -90 and 90")
    @Schema(description = "Start point latitude")
    private Double startLatitude;

    @DecimalMin(value = "-180.0", message = "Longitude must be between -180 and 180")
    @DecimalMax(value = "180.0", message = "Longitude must be between -180 and 180")
    @Schema(description = "Start point longitude")
    private Double startLongitude;

    @Schema(description = "End point latitude")
    private Double endLatitude;

    @Schema(description = "End point longitude")
    private Double endLongitude;

    @Schema(description = "Construction start date")
    private LocalDate constructionStartDate;

    @Schema(description = "Construction end date")
    private LocalDate constructionEndDate;

    @Schema(description = "Expected completion date")
    private LocalDate expectedCompletionDate;

    @Schema(description = "Construction status")
    private ConstructionStatus status;

    @Min(value = 1, message = "Design life must be at least 1 year")
    @Max(value = 200, message = "Design life cannot exceed 200 years")
    @Schema(description = "Design life in years")
    private Integer designLifeYears;

    @Schema(description = "Next scheduled inspection date")
    private LocalDate nextInspectionDate;

    @Schema(description = "Parent asset ID")
    private UUID parentAssetId;

    @Schema(description = "JSON metadata for flexible attributes")
    private String metadata;
}
