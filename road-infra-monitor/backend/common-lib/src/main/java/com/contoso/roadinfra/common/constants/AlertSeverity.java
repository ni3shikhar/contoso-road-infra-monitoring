package com.contoso.roadinfra.common.constants;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum AlertSeverity {
    INFO("Info", 0),
    LOW("Low", 1),
    MEDIUM("Medium", 2),
    HIGH("High", 3),
    CRITICAL("Critical", 4);

    private final String displayName;
    private final int level;

    AlertSeverity(String displayName, int level) {
        this.displayName = displayName;
        this.level = level;
    }

    @JsonValue
    public String getDisplayName() {
        return displayName;
    }

    public int getLevel() {
        return level;
    }

    @JsonCreator
    public static AlertSeverity fromDisplayName(String displayName) {
        if (displayName == null) {
            return null;
        }
        for (AlertSeverity severity : values()) {
            if (severity.displayName.equalsIgnoreCase(displayName) || severity.name().equalsIgnoreCase(displayName)) {
                return severity;
            }
        }
        throw new IllegalArgumentException("Unknown alert severity: " + displayName);
    }

    public static AlertSeverity fromLevel(int level) {
        for (AlertSeverity severity : values()) {
            if (severity.level == level) {
                return severity;
            }
        }
        throw new IllegalArgumentException("Unknown severity level: " + level);
    }

    public boolean isHigherThan(AlertSeverity other) {
        return this.level > other.level;
    }
}
