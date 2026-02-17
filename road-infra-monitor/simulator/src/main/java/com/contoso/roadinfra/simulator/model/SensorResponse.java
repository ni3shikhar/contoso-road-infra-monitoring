package com.contoso.roadinfra.simulator.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Response model for sensor data from the sensor-service API.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class SensorResponse {
    private UUID id;
    private String sensorCode;
    private String sensorType;
    private String manufacturer;
    private String model;
    private String status;
    private Double latitude;
    private Double longitude;
    private Double elevation;
    private UUID assetId;
    private String assetType;
    private String locationDescription;
    private Double batteryLevel;
    private Double signalStrength;
    private String firmwareVersion;
    private Double minThreshold;
    private Double maxThreshold;
    private String unit;
    private Double currentValue;
    private Instant lastDataReceivedAt;
}
