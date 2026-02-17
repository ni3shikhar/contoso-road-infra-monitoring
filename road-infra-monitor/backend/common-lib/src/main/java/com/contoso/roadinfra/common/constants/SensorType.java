package com.contoso.roadinfra.common.constants;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum SensorType {
    STRAIN_GAUGE("Strain Gauge"),
    ACCELEROMETER("Accelerometer"),
    TEMPERATURE("Temperature"),
    DISPLACEMENT("Displacement"),
    CRACK_METER("Crack Meter"),
    CRACK_SENSOR("Crack Sensor"),
    TILTMETER("Tiltmeter"),
    GPS("GPS"),
    CAMERA("Camera"),
    CCTV("CCTV"),
    MOISTURE("Moisture"),
    HUMIDITY("Humidity"),
    AIR_QUALITY("Air Quality"),
    WEATHER_STATION("Weather Station"),
    TRAFFIC_COUNTER("Traffic Counter"),
    WEIGHT_IN_MOTION("Weight In Motion"),
    OTHER("Other");

    private final String displayName;

    SensorType(String displayName) {
        this.displayName = displayName;
    }

    @JsonValue
    public String getDisplayName() {
        return displayName;
    }

    @JsonCreator
    public static SensorType fromDisplayName(String displayName) {
        if (displayName == null) {
            return null;
        }
        for (SensorType type : values()) {
            if (type.displayName.equalsIgnoreCase(displayName) || type.name().equalsIgnoreCase(displayName)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown sensor type: " + displayName);
    }
}
