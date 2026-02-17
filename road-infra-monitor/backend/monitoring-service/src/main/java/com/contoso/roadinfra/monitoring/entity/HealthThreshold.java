package com.contoso.roadinfra.monitoring.entity;

import com.contoso.roadinfra.common.constants.AssetType;
import com.contoso.roadinfra.common.constants.SensorType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

/**
 * Defines health thresholds for sensor readings per asset type.
 * Used to compute health scores and trigger alerts.
 */
@Entity
@Table(name = "health_thresholds", 
       uniqueConstraints = @UniqueConstraint(
           name = "uk_threshold_asset_sensor_metric",
           columnNames = {"asset_type", "sensor_type", "metric_name"}
       ),
       indexes = {
           @Index(name = "idx_threshold_asset_type", columnList = "asset_type"),
           @Index(name = "idx_threshold_sensor_type", columnList = "sensor_type")
       })
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HealthThreshold {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "asset_type", nullable = false, length = 50)
    private AssetType assetType;

    @Enumerated(EnumType.STRING)
    @Column(name = "sensor_type", nullable = false, length = 50)
    private SensorType sensorType;

    @Column(name = "metric_name", nullable = false, length = 100)
    private String metricName;

    /**
     * Lower warning threshold - below this is warning zone.
     */
    @Column(name = "warning_low")
    private Double warningLow;

    /**
     * Upper warning threshold - above this is warning zone.
     */
    @Column(name = "warning_high")
    private Double warningHigh;

    /**
     * Lower critical threshold - below this is critical zone.
     */
    @Column(name = "critical_low")
    private Double criticalLow;

    /**
     * Upper critical threshold - above this is critical zone.
     */
    @Column(name = "critical_high")
    private Double criticalHigh;

    @Column(length = 20)
    private String unit;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Builder.Default
    private Boolean enabled = true;

    @Column(name = "created_by", length = 100)
    private String createdBy;

    @Column(name = "updated_by", length = 100)
    private String updatedBy;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;

    /**
     * Evaluate a value against thresholds.
     * @return score from 0-100 based on how far the value is from thresholds
     */
    public double evaluateScore(Double value) {
        if (value == null) return 50.0; // Unknown value gets neutral score

        // Check critical thresholds first
        if (criticalLow != null && value < criticalLow) {
            return Math.max(0, 20 * (value / criticalLow));
        }
        if (criticalHigh != null && value > criticalHigh) {
            return Math.max(0, 20 * (criticalHigh / value));
        }

        // Check warning thresholds
        if (warningLow != null && value < warningLow) {
            double ratio = (value - (criticalLow != null ? criticalLow : 0)) / 
                          (warningLow - (criticalLow != null ? criticalLow : 0));
            return 20 + (20 * ratio);
        }
        if (warningHigh != null && value > warningHigh) {
            double ratio = (criticalHigh != null ? criticalHigh : warningHigh * 2) - value;
            double range = (criticalHigh != null ? criticalHigh : warningHigh * 2) - warningHigh;
            return 20 + (20 * (ratio / range));
        }

        // Value is in healthy range
        return 100.0;
    }

    /**
     * Check if a value exceeds critical thresholds.
     */
    public boolean isCritical(Double value) {
        if (value == null) return false;
        return (criticalLow != null && value < criticalLow) ||
               (criticalHigh != null && value > criticalHigh);
    }

    /**
     * Check if a value exceeds warning thresholds.
     */
    public boolean isWarning(Double value) {
        if (value == null) return false;
        if (isCritical(value)) return false;
        return (warningLow != null && value < warningLow) ||
               (warningHigh != null && value > warningHigh);
    }
}
