package com.contoso.roadinfra.analytics.constants;

/**
 * Trend direction for KPI values.
 */
public enum Trend {
    /** Value is increasing over time */
    INCREASING("Increasing", "↑", true),
    
    /** Value is decreasing over time */
    DECREASING("Decreasing", "↓", false),
    
    /** Value is stable (within threshold) */
    STABLE("Stable", "→", true),
    
    /** Significant spike detected */
    SPIKE("Spike", "⬆", false),
    
    /** Significant drop detected */
    DROP("Drop", "⬇", false),
    
    /** Trend cannot be determined */
    UNKNOWN("Unknown", "?", true);

    private final String displayName;
    private final String symbol;
    private final boolean positive;

    Trend(String displayName, String symbol, boolean positive) {
        this.displayName = displayName;
        this.symbol = symbol;
        this.positive = positive;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getSymbol() {
        return symbol;
    }

    public boolean isPositive() {
        return positive;
    }

    /**
     * Determine trend from percentage change.
     */
    public static Trend fromPercentageChange(double percentageChange) {
        if (percentageChange > 20) return SPIKE;
        if (percentageChange < -20) return DROP;
        if (percentageChange > 5) return INCREASING;
        if (percentageChange < -5) return DECREASING;
        return STABLE;
    }
}
