package com.contoso.roadinfra.monitoring.dto;

import com.contoso.roadinfra.common.constants.AssetType;
import com.contoso.roadinfra.common.constants.SensorType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Health threshold response")
public class HealthThresholdResponse {

    @Schema(description = "Threshold ID")
    private UUID id;

    @Schema(description = "Asset type this threshold applies to")
    private AssetType assetType;

    @Schema(description = "Sensor type this threshold applies to")
    private SensorType sensorType;

    @Schema(description = "Metric name")
    private String metricName;

    @Schema(description = "Lower warning threshold")
    private Double warningLow;

    @Schema(description = "Upper warning threshold")
    private Double warningHigh;

    @Schema(description = "Lower critical threshold")
    private Double criticalLow;

    @Schema(description = "Upper critical threshold")
    private Double criticalHigh;

    @Schema(description = "Unit of measurement")
    private String unit;

    @Schema(description = "Description of this threshold")
    private String description;

    @Schema(description = "Whether this threshold is enabled")
    private Boolean enabled;
}
