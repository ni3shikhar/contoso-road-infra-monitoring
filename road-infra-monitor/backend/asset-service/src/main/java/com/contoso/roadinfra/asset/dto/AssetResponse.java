package com.contoso.roadinfra.asset.dto;

import com.contoso.roadinfra.asset.constants.ConstructionStatus;
import com.contoso.roadinfra.common.constants.AssetType;
import com.contoso.roadinfra.common.constants.HealthStatus;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Asset response")
public class AssetResponse {

    @Schema(description = "Asset ID")
    private UUID id;

    @Schema(description = "Unique asset code", example = "RD-SEC-001")
    private String assetCode;

    @Schema(description = "Asset name")
    private String name;

    @Schema(description = "Type of the asset")
    private AssetType assetType;

    @Schema(description = "Detailed description")
    private String description;

    @Schema(description = "Start chainage in meters")
    private Double startChainage;

    @Schema(description = "End chainage in meters")
    private Double endChainage;

    @Schema(description = "Length in meters")
    private Double length;

    @Schema(description = "Start latitude")
    private Double startLatitude;

    @Schema(description = "Start longitude")
    private Double startLongitude;

    @Schema(description = "End latitude")
    private Double endLatitude;

    @Schema(description = "End longitude")
    private Double endLongitude;

    @JsonFormat(pattern = "yyyy-MM-dd")
    @Schema(description = "Construction start date")
    private LocalDate constructionStartDate;

    @JsonFormat(pattern = "yyyy-MM-dd")
    @Schema(description = "Construction end date")
    private LocalDate constructionEndDate;

    @JsonFormat(pattern = "yyyy-MM-dd")
    @Schema(description = "Expected completion date")
    private LocalDate expectedCompletionDate;

    @Schema(description = "Completion percentage", example = "75.5")
    private Double completionPercentage;

    @Schema(description = "Construction status")
    private ConstructionStatus status;

    @Schema(description = "Health status")
    private HealthStatus healthStatus;

    @Schema(description = "Design life in years")
    private Integer designLifeYears;

    @JsonFormat(pattern = "yyyy-MM-dd")
    @Schema(description = "Last inspection date")
    private LocalDate lastInspectionDate;

    @JsonFormat(pattern = "yyyy-MM-dd")
    @Schema(description = "Next inspection date")
    private LocalDate nextInspectionDate;

    @Schema(description = "Parent asset ID")
    private UUID parentAssetId;

    @Schema(description = "JSON metadata")
    private String metadata;

    // Computed fields
    @Schema(description = "Whether inspection is overdue")
    private boolean inspectionOverdue;

    @Schema(description = "Whether construction is delayed")
    private boolean delayed;

    @Schema(description = "Number of child assets")
    private Integer childAssetCount;

    @Schema(description = "Number of sensors attached")
    private Integer sensorCount;

    @Schema(description = "Child assets (only populated in detail view)")
    private List<AssetResponse> children;

    @Schema(description = "Created by user")
    private String createdBy;

    @Schema(description = "Last updated by user")
    private String updatedBy;

    @Schema(description = "Created timestamp")
    private Instant createdAt;

    @Schema(description = "Updated timestamp")
    private Instant updatedAt;
}
