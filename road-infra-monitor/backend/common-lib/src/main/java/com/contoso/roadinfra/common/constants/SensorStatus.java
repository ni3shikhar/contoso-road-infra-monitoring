package com.contoso.roadinfra.common.constants;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Status of an IoT sensor in the road infrastructure monitoring system.
 */
public enum SensorStatus {
    ACTIVE("Active"),
    INACTIVE("Inactive"),
    MAINTENANCE("Maintenance"),
    FAULTY("Faulty"),
    DECOMMISSIONED("Decommissioned"),
    OFFLINE("Offline");

    private final String displayName;

    SensorStatus(String displayName) {
        this.displayName = displayName;
    }

    @JsonValue
    public String getDisplayName() {
        return displayName;
    }

    @JsonCreator
    public static SensorStatus fromDisplayName(String displayName) {
        if (displayName == null) {
            return null;
        }
        for (SensorStatus status : values()) {
            if (status.displayName.equalsIgnoreCase(displayName) || status.name().equalsIgnoreCase(displayName)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown sensor status: " + displayName);
    }
    
    public boolean isOperational() {
        return this == ACTIVE;
    }
    
    public boolean requiresAttention() {
        return this == MAINTENANCE || this == FAULTY || this == OFFLINE;
    }
}
