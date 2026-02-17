package com.contoso.roadinfra.asset.constants;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Types of infrastructure inspections.
 */
public enum InspectionType {
    ROUTINE("Routine", "Regular scheduled inspection"),
    DETAILED("Detailed", "Comprehensive structural inspection"),
    EMERGENCY("Emergency", "Inspection after unexpected event"),
    POST_EVENT("Post-Event", "Inspection after weather or seismic event");

    private final String displayName;
    private final String description;

    InspectionType(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    @JsonValue
    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    public static InspectionType fromDisplayName(String displayName) {
        for (InspectionType type : values()) {
            if (type.displayName.equalsIgnoreCase(displayName)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown inspection type: " + displayName);
    }

    /**
     * Get recommended interval in days for next inspection.
     */
    public int getRecommendedIntervalDays() {
        return switch (this) {
            case ROUTINE -> 90;        // 3 months
            case DETAILED -> 365;      // 1 year
            case EMERGENCY -> 30;      // 1 month follow-up
            case POST_EVENT -> 14;     // 2 weeks follow-up
        };
    }
}
