package com.contoso.roadinfra.sensor.dto;

import com.contoso.roadinfra.common.constants.AssetType;
import com.contoso.roadinfra.common.constants.SensorStatus;
import com.contoso.roadinfra.common.constants.SensorType;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Response DTO for sensor data.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Sensor response data")
public class SensorResponse {

    @Schema(description = "Unique sensor identifier")
    private UUID id;

    @Schema(description = "Unique sensor code", example = "SG-BR-001")
    private String sensorCode;

    @Schema(description = "Type of sensor")
    private SensorType sensorType;

    @Schema(description = "Sensor manufacturer", example = "Honeywell")
    private String manufacturer;

    @Schema(description = "Sensor model", example = "HSM-200")
    private String model;

    @Schema(description = "Date when the sensor was installed")
    private LocalDate installationDate;

    @Schema(description = "Date of last calibration")
    private LocalDate lastCalibrationDate;

    @Schema(description = "Days between calibrations")
    private Integer calibrationIntervalDays;

    @Schema(description = "Whether calibration is due")
    private Boolean calibrationDue;

    @Schema(description = "Current sensor status")
    private SensorStatus status;

    @Schema(description = "Geographic latitude")
    private Double latitude;

    @Schema(description = "Geographic longitude")
    private Double longitude;

    @Schema(description = "Elevation in meters")
    private Double elevation;

    @Schema(description = "ID of the asset this sensor is attached to")
    private UUID assetId;

    @Schema(description = "Type of asset")
    private AssetType assetType;

    @Schema(description = "Human-readable location description")
    private String locationDescription;

    @Schema(description = "Battery level percentage")
    private Double batteryLevel;

    @Schema(description = "Signal strength in dBm")
    private Double signalStrength;

    @Schema(description = "Current firmware version")
    private String firmwareVersion;

    @Schema(description = "Minimum threshold for alerts")
    private Double minThreshold;

    @Schema(description = "Maximum threshold for alerts")
    private Double maxThreshold;

    @Schema(description = "Unit of measurement")
    private String unit;

    @Schema(description = "Current sensor reading value")
    private Double currentValue;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSZ", timezone = "UTC")
    @Schema(description = "Last data received timestamp")
    private Instant lastDataReceivedAt;

    @Schema(description = "User who created this record")
    private String createdBy;

    @Schema(description = "User who last updated this record")
    private String updatedBy;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSZ", timezone = "UTC")
    @Schema(description = "Record creation timestamp")
    private Instant createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSZ", timezone = "UTC")
    @Schema(description = "Record last update timestamp")
    private Instant updatedAt;
}
