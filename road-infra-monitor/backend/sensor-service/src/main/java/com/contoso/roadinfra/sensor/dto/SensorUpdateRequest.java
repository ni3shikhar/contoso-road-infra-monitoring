package com.contoso.roadinfra.sensor.dto;

import com.contoso.roadinfra.common.constants.AssetType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Request DTO for updating an existing sensor.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to update an existing sensor")
public class SensorUpdateRequest {

    @Size(max = 100, message = "Manufacturer must not exceed 100 characters")
    @Schema(description = "Sensor manufacturer", example = "Honeywell")
    private String manufacturer;

    @Size(max = 100, message = "Model must not exceed 100 characters")
    @Schema(description = "Sensor model", example = "HSM-200")
    private String model;

    @Schema(description = "Date of last calibration")
    private LocalDate lastCalibrationDate;

    @Min(value = 1, message = "Calibration interval must be at least 1 day")
    @Max(value = 3650, message = "Calibration interval must not exceed 10 years")
    @Schema(description = "Days between calibrations", example = "90")
    private Integer calibrationIntervalDays;

    @DecimalMin(value = "-90.0", message = "Latitude must be between -90 and 90")
    @DecimalMax(value = "90.0", message = "Latitude must be between -90 and 90")
    @Schema(description = "Geographic latitude", example = "47.6062")
    private Double latitude;

    @DecimalMin(value = "-180.0", message = "Longitude must be between -180 and 180")
    @DecimalMax(value = "180.0", message = "Longitude must be between -180 and 180")
    @Schema(description = "Geographic longitude", example = "-122.3321")
    private Double longitude;

    @Schema(description = "Elevation in meters", example = "15.5")
    private Double elevation;

    @Schema(description = "ID of the asset this sensor is attached to")
    private UUID assetId;

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

    @DecimalMin(value = "0.0", message = "Battery level must be between 0 and 100")
    @DecimalMax(value = "100.0", message = "Battery level must be between 0 and 100")
    @Schema(description = "Battery level percentage", example = "85.5")
    private Double batteryLevel;

    @Schema(description = "Signal strength in dBm", example = "-65.0")
    private Double signalStrength;
}
