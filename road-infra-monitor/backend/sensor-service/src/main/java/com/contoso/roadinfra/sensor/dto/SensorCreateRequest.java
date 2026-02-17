package com.contoso.roadinfra.sensor.dto;

import com.contoso.roadinfra.common.constants.AssetType;
import com.contoso.roadinfra.common.constants.SensorType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Request DTO for creating a new sensor.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to create a new sensor")
public class SensorCreateRequest {

    @NotBlank(message = "Sensor code is required")
    @Size(max = 50, message = "Sensor code must not exceed 50 characters")
    @Pattern(regexp = "^[A-Z]{2,3}-[A-Z]{2,3}-\\d{3}$", message = "Sensor code must follow pattern like 'SG-BR-001'")
    @Schema(description = "Unique sensor code", example = "SG-BR-001")
    private String sensorCode;

    @NotNull(message = "Sensor type is required")
    @Schema(description = "Type of sensor")
    private SensorType sensorType;

    @NotBlank(message = "Manufacturer is required")
    @Size(max = 100, message = "Manufacturer must not exceed 100 characters")
    @Schema(description = "Sensor manufacturer", example = "Honeywell")
    private String manufacturer;

    @NotBlank(message = "Model is required")
    @Size(max = 100, message = "Model must not exceed 100 characters")
    @Schema(description = "Sensor model", example = "HSM-200")
    private String model;

    @NotNull(message = "Installation date is required")
    @PastOrPresent(message = "Installation date cannot be in the future")
    @Schema(description = "Date when the sensor was installed")
    private LocalDate installationDate;

    @Schema(description = "Date of last calibration")
    private LocalDate lastCalibrationDate;

    @Min(value = 1, message = "Calibration interval must be at least 1 day")
    @Max(value = 3650, message = "Calibration interval must not exceed 10 years")
    @Schema(description = "Days between calibrations", example = "90")
    private Integer calibrationIntervalDays;

    @NotNull(message = "Latitude is required")
    @DecimalMin(value = "-90.0", message = "Latitude must be between -90 and 90")
    @DecimalMax(value = "90.0", message = "Latitude must be between -90 and 90")
    @Schema(description = "Geographic latitude", example = "47.6062")
    private Double latitude;

    @NotNull(message = "Longitude is required")
    @DecimalMin(value = "-180.0", message = "Longitude must be between -180 and 180")
    @DecimalMax(value = "180.0", message = "Longitude must be between -180 and 180")
    @Schema(description = "Geographic longitude", example = "-122.3321")
    private Double longitude;

    @Schema(description = "Elevation in meters", example = "15.5")
    private Double elevation;

    @NotNull(message = "Asset ID is required")
    @Schema(description = "ID of the asset this sensor is attached to")
    private UUID assetId;

    @NotNull(message = "Asset type is required")
    @Schema(description = "Type of asset")
    private AssetType assetType;

    @Size(max = 255, message = "Location description must not exceed 255 characters")
    @Schema(description = "Human-readable location description", example = "Bridge Pier 3, North Face")
    private String locationDescription;

    @Size(max = 50, message = "Firmware version must not exceed 50 characters")
    @Schema(description = "Current firmware version", example = "2.1.4")
    private String firmwareVersion;

    @Schema(description = "Minimum threshold for alerts")
    private Double minThreshold;

    @Schema(description = "Maximum threshold for alerts")
    private Double maxThreshold;

    @Size(max = 30, message = "Unit must not exceed 30 characters")
    @Schema(description = "Unit of measurement", example = "microstrain")
    private String unit;
}
