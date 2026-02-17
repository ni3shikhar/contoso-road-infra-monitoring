package com.contoso.roadinfra.asset.constants;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Status of construction milestones.
 */
public enum MilestoneStatus {
    PENDING("Pending"),
    IN_PROGRESS("In Progress"),
    COMPLETED("Completed"),
    DELAYED("Delayed"),
    CANCELLED("Cancelled");

    private final String displayName;

    MilestoneStatus(String displayName) {
        this.displayName = displayName;
    }

    @JsonValue
    public String getDisplayName() {
        return displayName;
    }

    public static MilestoneStatus fromDisplayName(String displayName) {
        for (MilestoneStatus status : values()) {
            if (status.displayName.equalsIgnoreCase(displayName)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown milestone status: " + displayName);
    }

    /**
     * Check if the milestone is in a terminal state.
     */
    public boolean isTerminal() {
        return this == COMPLETED || this == CANCELLED;
    }

    /**
     * Check if the milestone counts towards completion percentage.
     */
    public boolean countsTowardsCompletion() {
        return this == COMPLETED;
    }
}
