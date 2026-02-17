package com.contoso.roadinfra.common.constants;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum HealthStatus {
    HEALTHY("Healthy"),
    FAIR("Fair"),
    WARNING("Warning"),
    CRITICAL("Critical"),
    OFFLINE("Offline"),
    UNKNOWN("Unknown");

    private final String displayName;

    HealthStatus(String displayName) {
        this.displayName = displayName;
    }

    @JsonValue
    public String getDisplayName() {
        return displayName;
    }

    @JsonCreator
    public static HealthStatus fromDisplayName(String displayName) {
        if (displayName == null) {
            return null;
        }
        for (HealthStatus status : values()) {
            if (status.displayName.equalsIgnoreCase(displayName) || status.name().equalsIgnoreCase(displayName)) {
                return status;
            }
        }
        return UNKNOWN;
    }

    public int getSeverityLevel() {
        return switch (this) {
            case HEALTHY -> 0;
            case FAIR -> 1;
            case WARNING -> 2;
            case CRITICAL -> 3;
            case OFFLINE -> 4;
            case UNKNOWN -> -1;
        };
    }
}
