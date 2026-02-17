package com.contoso.roadinfra.analytics.entity;

import com.contoso.roadinfra.analytics.constants.Trend;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Daily snapshot of KPI values for historical tracking.
 */
@Entity
@Table(name = "kpi_snapshots", indexes = {
        @Index(name = "idx_snapshot_metric", columnList = "metric_name"),
        @Index(name = "idx_snapshot_date", columnList = "snapshot_date"),
        @Index(name = "idx_snapshot_category", columnList = "category")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_snapshot_metric_date", columnNames = {"metric_name", "snapshot_date", "asset_id"})
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KpiSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "metric_name", nullable = false)
    private String metricName;

    @Column(name = "display_name")
    private String displayName;

    @Column(nullable = false)
    private String category;

    @Column(name = "snapshot_date", nullable = false)
    private LocalDate snapshotDate;

    @Column(nullable = false)
    private Double value;

    @Column(name = "previous_value")
    private Double previousValue;

    @Column(name = "week_ago_value")
    private Double weekAgoValue;

    @Column(name = "month_ago_value")
    private Double monthAgoValue;

    @Column(name = "target_value")
    private Double targetValue;

    private String unit;

    @Column(name = "percentage_change")
    private Double percentageChange;

    @Enumerated(EnumType.STRING)
    private Trend trend;

    @Column(name = "on_target")
    private Boolean onTarget;

    /** Optional: associated asset ID */
    @Column(name = "asset_id")
    private UUID assetId;

    /** Notes or context for this snapshot */
    @Column(length = 1000)
    private String notes;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    /**
     * Calculate trend based on current and previous values.
     */
    public void calculateTrend() {
        if (previousValue == null || value == null) {
            this.trend = Trend.STABLE;
            this.percentageChange = 0.0;
            return;
        }
        
        if (previousValue == 0) {
            this.percentageChange = value > 0 ? 100.0 : 0.0;
        } else {
            this.percentageChange = ((value - previousValue) / Math.abs(previousValue)) * 100.0;
        }
        
        if (percentageChange > 5) {
            this.trend = Trend.INCREASING;
        } else if (percentageChange < -5) {
            this.trend = Trend.DECREASING;
        } else {
            this.trend = Trend.STABLE;
        }
    }

    /**
     * Check if KPI is meeting its target.
     */
    public void evaluateTarget() {
        if (targetValue == null || value == null) {
            this.onTarget = null;
            return;
        }
        
        // For most KPIs, higher is better (e.g., uptime, health score)
        // This can be customized per metric
        this.onTarget = value >= targetValue;
    }
}
