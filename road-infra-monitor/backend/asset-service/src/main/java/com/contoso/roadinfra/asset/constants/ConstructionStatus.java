package com.contoso.roadinfra.asset.constants;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Represents the construction status of an infrastructure asset.
 */
public enum ConstructionStatus {
    PLANNED("Planned"),
    IN_PROGRESS("In Progress"),
    COMPLETED("Completed"),
    UNDER_MAINTENANCE("Under Maintenance"),
    DECOMMISSIONED("Decommissioned");

    private final String displayName;

    ConstructionStatus(String displayName) {
        this.displayName = displayName;
    }

    @JsonValue
    public String getDisplayName() {
        return displayName;
    }

    public static ConstructionStatus fromDisplayName(String displayName) {
        for (ConstructionStatus status : values()) {
            if (status.displayName.equalsIgnoreCase(displayName)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown construction status: " + displayName);
    }

    /**
     * Check if the asset is actively under construction.
     */
    public boolean isActive() {
        return this == PLANNED || this == IN_PROGRESS || this == UNDER_MAINTENANCE;
    }

    /**
     * Check if progress updates are allowed.
     */
    public boolean allowsProgressUpdate() {
        return this == IN_PROGRESS;
    }
}
