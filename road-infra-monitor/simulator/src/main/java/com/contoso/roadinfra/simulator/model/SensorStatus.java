package com.contoso.roadinfra.simulator.model;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Status of a sensor in the system.
 */
public enum SensorStatus {
    ACTIVE("Active"),
    INACTIVE("Inactive"),
    FAULTY("Faulty"),
    MAINTENANCE("Under Maintenance"),
    DECOMMISSIONED("Decommissioned");

    private final String displayName;

    SensorStatus(String displayName) {
        this.displayName = displayName;
    }

    @JsonValue
    public String getDisplayName() {
        return displayName;
    }

    public static SensorStatus fromDisplayName(String displayName) {
        for (SensorStatus status : values()) {
            if (status.displayName.equalsIgnoreCase(displayName) || status.name().equalsIgnoreCase(displayName)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown sensor status: " + displayName);
    }
}
