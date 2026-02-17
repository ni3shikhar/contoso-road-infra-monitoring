package com.contoso.roadinfra.monitoring.dto;

import com.contoso.roadinfra.common.constants.AssetType;
import com.contoso.roadinfra.common.constants.SensorType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to update health threshold")
public class HealthThresholdUpdateRequest {

    @Schema(description = "Asset type for this threshold")
    private AssetType assetType;

    @Schema(description = "Sensor type for this threshold")
    private SensorType sensorType;

    @Schema(description = "Metric name for this threshold")
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

    @NotNull(message = "Enabled status is required")
    @Schema(description = "Whether this threshold is enabled")
    private Boolean enabled;
}
