package com.contoso.roadinfra.common.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "KPI data transfer object")
public class KpiDTO {

    @Schema(description = "Unique KPI record identifier")
    private UUID id;

    @Schema(description = "KPI metric name", example = "MEAN_TIME_TO_REPAIR")
    private String metricName;

    @Schema(description = "KPI display name", example = "Mean Time to Repair")
    private String displayName;

    @Schema(description = "KPI category", example = "MAINTENANCE")
    private String category;

    @Schema(description = "Current metric value")
    private Double value;

    @Schema(description = "Previous period value for comparison")
    private Double previousValue;

    @Schema(description = "Target/goal value")
    private Double targetValue;

    @Schema(description = "Unit of measurement", example = "hours")
    private String unit;

    @Schema(description = "Percentage change from previous period")
    private Double percentageChange;

    @Schema(description = "Trend indicator: UP, DOWN, STABLE")
    private String trend;

    @Schema(description = "Whether current value meets target")
    private Boolean onTarget;

    @Schema(description = "Associated asset ID (null for system-wide KPIs)")
    private UUID assetId;

    @Schema(description = "Asset name if applicable")
    private String assetName;

    @Schema(description = "Time period for this KPI", example = "DAILY")
    private String period;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Schema(description = "Start of measurement period")
    private LocalDateTime periodStart;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Schema(description = "End of measurement period")
    private LocalDateTime periodEnd;

    @Schema(description = "Historical data points for trend visualization")
    private Map<String, Double> historicalData;

    @Schema(description = "Breakdown by subcategory")
    private Map<String, Double> breakdown;

    @Schema(description = "Additional metadata")
    private Map<String, Object> metadata;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Schema(description = "When this KPI was calculated")
    private LocalDateTime calculatedAt;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Schema(description = "Record creation timestamp")
    private LocalDateTime createdAt;
}
