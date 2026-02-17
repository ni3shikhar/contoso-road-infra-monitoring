package com.contoso.roadinfra.simulator.model;

/**
 * Types of anomalies that can be injected into sensor readings.
 */
public enum AnomalyType {
    /**
     * Gradual drift from normal baseline over time.
     */
    GRADUAL_DRIFT,

    /**
     * Sudden spike in sensor value.
     */
    SUDDEN_SPIKE,

    /**
     * Sensor stuck at a constant value (malfunction simulation).
     */
    STUCK_VALUE,

    /**
     * Erratic readings with high variance (noise).
     */
    ERRATIC_NOISE
}
