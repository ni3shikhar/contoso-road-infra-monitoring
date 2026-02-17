package com.contoso.roadinfra.common.constants;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Data quality indicator for sensor readings.
 */
public enum DataQuality {
    GOOD("Good", 100),
    SUSPECT("Suspect", 50),
    BAD("Bad", 0),
    INTERPOLATED("Interpolated", 75);

    private final String displayName;
    private final int confidenceScore;

    DataQuality(String displayName, int confidenceScore) {
        this.displayName = displayName;
        this.confidenceScore = confidenceScore;
    }

    @JsonValue
    public String getDisplayName() {
        return displayName;
    }

    public int getConfidenceScore() {
        return confidenceScore;
    }

    @JsonCreator
    public static DataQuality fromDisplayName(String displayName) {
        if (displayName == null) {
            return null;
        }
        for (DataQuality quality : values()) {
            if (quality.displayName.equalsIgnoreCase(displayName) || 
                quality.name().equalsIgnoreCase(displayName)) {
                return quality;
            }
        }
        throw new IllegalArgumentException("Unknown data quality: " + displayName);
    }

    public boolean isReliable() {
        return this == GOOD || this == INTERPOLATED;
    }
}
