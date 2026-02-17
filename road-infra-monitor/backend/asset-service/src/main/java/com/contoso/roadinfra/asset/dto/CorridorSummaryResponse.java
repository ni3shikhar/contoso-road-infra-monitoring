package com.contoso.roadinfra.asset.dto;

import com.contoso.roadinfra.common.constants.AssetType;
import com.contoso.roadinfra.common.constants.HealthStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Summary of the entire corridor")
public class CorridorSummaryResponse {

    @Schema(description = "Total corridor length in meters", example = "2000.0")
    private Double totalLength;

    @Schema(description = "Total number of assets")
    private Integer totalAssets;

    @Schema(description = "Average completion percentage across all assets")
    private Double averageCompletionPercentage;

    @Schema(description = "Number of assets by type")
    private Map<AssetType, Long> assetCountByType;

    @Schema(description = "Number of assets by health status")
    private Map<HealthStatus, Long> assetCountByHealthStatus;

    @Schema(description = "Total number of sensors deployed")
    private Integer totalSensors;

    @Schema(description = "Number of assets with overdue inspections")
    private Integer overdueInspections;

    @Schema(description = "Number of delayed milestones")
    private Integer delayedMilestones;

    @Schema(description = "Number of assets in critical condition")
    private Integer criticalAssets;

    @Schema(description = "Overall corridor health")
    private HealthStatus overallHealth;
}
