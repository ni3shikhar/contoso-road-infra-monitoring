package com.contoso.roadinfra.common.dto;

import com.contoso.roadinfra.common.constants.HealthStatus;
import com.contoso.roadinfra.common.constants.SensorType;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
@Schema(description = "Sensor data transfer object")
public class SensorDTO {

    @Schema(description = "Unique sensor identifier", example = "550e8400-e29b-41d4-a716-446655440000")
    private UUID id;

    @NotBlank(message = "Sensor name is required")
    @Schema(description = "Sensor name", example = "Bridge-01-StrainGauge-A")
    private String name;

    @NotNull(message = "Sensor type is required")
    @Schema(description = "Type of sensor")
    private SensorType sensorType;

    @NotNull(message = "Asset ID is required")
    @Schema(description = "ID of the asset this sensor belongs to")
    private UUID assetId;

    @Schema(description = "Geographic latitude", example = "47.6062")
    private Double latitude;

    @Schema(description = "Geographic longitude", example = "-122.3321")
    private Double longitude;

    @Schema(description = "Installation location description", example = "North pier, section A")
    private String locationDescription;

    @Schema(description = "Current health status of the sensor")
    private HealthStatus status;

    @Schema(description = "Current sensor reading value")
    private Double currentValue;

    @Schema(description = "Unit of measurement", example = "microstrain")
    private String unit;

    @Schema(description = "Minimum threshold value")
    private Double minThreshold;

    @Schema(description = "Maximum threshold value")
    private Double maxThreshold;

    @Schema(description = "Firmware version", example = "2.1.4")
    private String firmwareVersion;

    @Schema(description = "Battery level percentage", example = "85")
    private Integer batteryLevel;

    @Schema(description = "Signal strength in dBm", example = "-65")
    private Integer signalStrength;

    @Schema(description = "Whether the sensor is active")
    private Boolean active;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Schema(description = "Installation date")
    private LocalDateTime installedAt;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Schema(description = "Last maintenance date")
    private LocalDateTime lastMaintenanceAt;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Schema(description = "Last data received timestamp")
    private LocalDateTime lastDataReceivedAt;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Schema(description = "Record creation timestamp")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Schema(description = "Record last update timestamp")
    private LocalDateTime updatedAt;
}
