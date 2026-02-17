package com.contoso.roadinfra.simulator.model;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Types of sensors deployed on the road corridor.
 */
public enum SensorType {
    STRAIN_GAUGE("Strain Gauge"),
    ACCELEROMETER("Accelerometer"),
    TEMPERATURE("Temperature"),
    DISPLACEMENT("Displacement"),
    CRACK_METER("Crack Meter"),
    TILTMETER("Tiltmeter"),
    GPS("GPS"),
    CAMERA("Camera"),
    MOISTURE("Moisture"),
    AIR_QUALITY("Air Quality"),
    FLOW_SENSOR("Flow Sensor"),
    LEVEL_SENSOR("Level Sensor"),
    IMPACT_SENSOR("Impact Sensor");

    private final String displayName;

    SensorType(String displayName) {
        this.displayName = displayName;
    }

    @JsonValue
    public String getDisplayName() {
        return displayName;
    }

    public static SensorType fromDisplayName(String displayName) {
        for (SensorType type : values()) {
            if (type.displayName.equalsIgnoreCase(displayName) || type.name().equalsIgnoreCase(displayName)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown sensor type: " + displayName);
    }
}
