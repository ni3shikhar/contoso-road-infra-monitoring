package com.contoso.roadinfra.simulator.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Represents a sensor with simulation state for generating realistic data.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SimulatedSensor {

    private UUID id;
    private String sensorCode;
    private SensorType sensorType;
    private String assetType;
    private UUID assetId;
    private SensorStatus status;
    private Double batteryLevel;
    private String unit;
    private Double minThreshold;
    private Double maxThreshold;

    // Simulation state
    private double baselineValue;
    private double currentDrift;
    private boolean inAnomalyMode;
    private AnomalyType currentAnomaly;
    private Instant anomalyStartTime;
    private Instant failureRecoveryTime;

    /**
     * Check if this sensor is battery-powered.
     */
    public boolean isBatteryPowered() {
        return batteryLevel != null && batteryLevel > 0;
    }

    /**
     * Check if the sensor is in a tunnel.
     */
    public boolean isInTunnel() {
        return "Tunnel".equalsIgnoreCase(assetType);
    }

    /**
     * Check if the sensor is currently operational for readings.
     */
    public boolean isOperational() {
        return status == SensorStatus.ACTIVE && 
               (failureRecoveryTime == null || Instant.now().isAfter(failureRecoveryTime));
    }
}
