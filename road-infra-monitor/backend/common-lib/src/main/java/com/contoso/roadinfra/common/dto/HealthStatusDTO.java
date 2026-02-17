package com.contoso.roadinfra.common.dto;

import com.contoso.roadinfra.common.constants.HealthStatus;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Health status data transfer object")
public class HealthStatusDTO {

    @Schema(description = "Unique health record identifier")
    private UUID id;

    @Schema(description = "ID of the asset being monitored")
    private UUID assetId;

    @Schema(description = "Name of the asset")
    private String assetName;

    @Schema(description = "ID of the sensor providing data")
    private UUID sensorId;

    @Schema(description = "Name of the sensor")
    private String sensorName;

    @Schema(description = "Current health status")
    private HealthStatus status;

    @Schema(description = "Previous health status")
    private HealthStatus previousStatus;

    @Schema(description = "Health score from 0-100", example = "78")
    private Integer healthScore;

    @Schema(description = "Detailed health metrics")
    private Map<String, Double> metrics;

    @Schema(description = "List of current issues or concerns")
    private List<String> issues;

    @Schema(description = "Recommended actions")
    private List<String> recommendations;

    @Schema(description = "Whether status has changed recently")
    private Boolean statusChanged;

    @Schema(description = "Duration of current status in minutes")
    private Long statusDurationMinutes;

    @Schema(description = "Trend indicator: IMPROVING, STABLE, DEGRADING")
    private String trend;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Schema(description = "When this health check was performed")
    private LocalDateTime checkedAt;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Schema(description = "When next health check is scheduled")
    private LocalDateTime nextCheckAt;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Schema(description = "Record creation timestamp")
    private LocalDateTime createdAt;
}
