package com.contoso.roadinfra.common.constants;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Types of alerts that can be generated for sensors.
 */
public enum SensorAlertType {
    THRESHOLD_BREACH("Threshold Breach"),
    OFFLINE("Offline"),
    LOW_BATTERY("Low Battery"),
    CALIBRATION_DUE("Calibration Due"),
    ANOMALY("Anomaly"),
    SIGNAL_WEAK("Signal Weak"),
    DATA_QUALITY_ISSUE("Data Quality Issue"),
    COMMUNICATION_ERROR("Communication Error");

    private final String displayName;

    SensorAlertType(String displayName) {
        this.displayName = displayName;
    }

    @JsonValue
    public String getDisplayName() {
        return displayName;
    }

    @JsonCreator
    public static SensorAlertType fromDisplayName(String displayName) {
        if (displayName == null) {
            return null;
        }
        for (SensorAlertType type : values()) {
            if (type.displayName.equalsIgnoreCase(displayName) || type.name().equalsIgnoreCase(displayName)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown sensor alert type: " + displayName);
    }
}
