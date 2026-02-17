package com.contoso.roadinfra.monitoring.dto;

import com.contoso.roadinfra.common.constants.AssetType;
import com.contoso.roadinfra.common.constants.HealthStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Asset health status response")
public class AssetHealthResponse {

    @Schema(description = "Record ID")
    private UUID id;

    @Schema(description = "Asset ID")
    private UUID assetId;

    @Schema(description = "Asset type")
    private AssetType assetType;

    @Schema(description = "Asset name")
    private String assetName;

    @Schema(description = "Timestamp of health check")
    private LocalDateTime timestamp;

    @Schema(description = "Overall health score (0-100)")
    private Double overallHealthScore;

    @Schema(description = "Structural health score (0-100)")
    private Double structuralScore;

    @Schema(description = "Environmental health score (0-100)")
    private Double environmentalScore;

    @Schema(description = "Operational health score (0-100)")
    private Double operationalScore;

    @Schema(description = "Current health status")
    private HealthStatus healthStatus;

    @Schema(description = "Number of active sensors")
    private Integer activeSensorCount;

    @Schema(description = "Total number of sensors")
    private Integer totalSensorCount;

    @Schema(description = "Number of faulty sensors")
    private Integer faultySensorCount;

    @Schema(description = "Number of active alerts")
    private Integer activeAlertCount;

    @Schema(description = "Health trend (IMPROVING, STABLE, DECLINING)")
    private String trend;

    @Schema(description = "Additional notes")
    private String notes;

    @Schema(description = "Sensor uptime percentage")
    public Double getSensorUptimePercentage() {
        if (totalSensorCount == null || totalSensorCount == 0) return 100.0;
        return (activeSensorCount != null ? activeSensorCount : 0) * 100.0 / totalSensorCount;
    }
}
