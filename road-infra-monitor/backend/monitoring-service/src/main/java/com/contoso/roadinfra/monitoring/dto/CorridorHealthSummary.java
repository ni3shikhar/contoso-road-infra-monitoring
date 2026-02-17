package com.contoso.roadinfra.monitoring.dto;

import com.contoso.roadinfra.common.constants.AssetType;
import com.contoso.roadinfra.common.constants.HealthStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Summary of health across the entire corridor")
public class CorridorHealthSummary {

    @Schema(description = "Timestamp of the summary")
    private LocalDateTime timestamp;

    @Schema(description = "Overall corridor health score (0-100)")
    private Double overallScore;

    @Schema(description = "Average health score across all assets")
    private Double averageHealthScore;

    @Schema(description = "Overall corridor health status")
    private HealthStatus overallStatus;

    @Schema(description = "Total number of assets monitored")
    private Integer totalAssets;

    @Schema(description = "Number of assets in HEALTHY status")
    private Integer healthyAssets;

    @Schema(description = "Count of healthy assets")
    private Integer healthyCount;

    @Schema(description = "Number of assets in FAIR status")
    private Integer fairAssets;

    @Schema(description = "Number of assets in WARNING status")
    private Integer warningAssets;

    @Schema(description = "Count of warning assets")
    private Integer warningCount;

    @Schema(description = "Number of assets in CRITICAL status")
    private Integer criticalAssets;

    @Schema(description = "Count of critical assets")
    private Integer criticalCount;

    @Schema(description = "Count of assets with unknown status")
    private Integer unknownCount;

    @Schema(description = "Total active sensors across corridor")
    private Integer totalActiveSensors;

    @Schema(description = "Total sensors across corridor")
    private Integer totalSensors;

    @Schema(description = "Total faulty sensors across corridor")
    private Integer totalFaultySensors;

    @Schema(description = "Sensor uptime percentage")
    private Double sensorUptime;

    @Schema(description = "Total active alerts across corridor")
    private Integer totalActiveAlerts;

    @Schema(description = "Number of recent alerts")
    private Integer recentAlertCount;

    @Schema(description = "Health scores by asset type")
    private Map<String, Double> scoresByAssetType;

    @Schema(description = "Health scores by asset type (typed)")
    private Map<AssetType, Double> healthByAssetType;

    @Schema(description = "Asset counts by status")
    private Map<HealthStatus, Integer> assetCountsByStatus;

    @Schema(description = "Timestamp of summary calculation")
    private LocalDateTime calculatedAt;

    @Schema(description = "Sensor uptime percentage across corridor")
    public Double getSensorUptimePercentage() {
        if (totalSensors == null || totalSensors == 0) return 100.0;
        return (totalActiveSensors != null ? totalActiveSensors : 0) * 100.0 / totalSensors;
    }

    @Schema(description = "Percentage of healthy assets")
    public Double getHealthyPercentage() {
        if (totalAssets == null || totalAssets == 0) return 0.0;
        return (healthyAssets != null ? healthyAssets : 0) * 100.0 / totalAssets;
    }
}
