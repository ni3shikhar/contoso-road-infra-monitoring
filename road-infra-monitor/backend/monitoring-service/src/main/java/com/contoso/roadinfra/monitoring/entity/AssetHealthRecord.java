package com.contoso.roadinfra.monitoring.entity;

import com.contoso.roadinfra.common.constants.AssetType;
import com.contoso.roadinfra.common.constants.HealthStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Represents the current health status of an infrastructure asset.
 * Computed from aggregated sensor data and updated periodically.
 */
@Entity
@Table(name = "asset_health_records", indexes = {
        @Index(name = "idx_asset_health_asset_id", columnList = "asset_id"),
        @Index(name = "idx_asset_health_status", columnList = "health_status"),
        @Index(name = "idx_asset_health_timestamp", columnList = "timestamp"),
        @Index(name = "idx_asset_health_score", columnList = "overall_health_score")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssetHealthRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "asset_id", nullable = false)
    private UUID assetId;

    @Enumerated(EnumType.STRING)
    @Column(name = "asset_type", nullable = false, length = 50)
    private AssetType assetType;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    /**
     * Overall health score computed from sensor data (0-100).
     * 0-20: CRITICAL, 21-40: WARNING, 41-70: FAIR, 71-100: HEALTHY
     */
    @Column(name = "overall_health_score", nullable = false)
    private Double overallHealthScore;

    /**
     * Structural health score from strain, vibration, displacement sensors.
     */
    @Column(name = "structural_score")
    private Double structuralScore;

    /**
     * Environmental score from temperature, humidity, wind sensors.
     */
    @Column(name = "environmental_score")
    private Double environmentalScore;

    /**
     * Operational score from traffic, load, and operational sensors.
     */
    @Column(name = "operational_score")
    private Double operationalScore;

    @Enumerated(EnumType.STRING)
    @Column(name = "health_status", nullable = false, length = 20)
    private HealthStatus healthStatus;

    @Column(name = "active_sensor_count")
    @Builder.Default
    private Integer activeSensorCount = 0;

    @Column(name = "total_sensor_count")
    @Builder.Default
    private Integer totalSensorCount = 0;

    @Column(name = "faulty_sensor_count")
    @Builder.Default
    private Integer faultySensorCount = 0;

    @Column(name = "active_alert_count")
    @Builder.Default
    private Integer activeAlertCount = 0;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;

    /**
     * Calculate health status from overall score.
     */
    public static HealthStatus calculateStatus(Double score) {
        if (score == null) return HealthStatus.UNKNOWN;
        if (score <= 20) return HealthStatus.CRITICAL;
        if (score <= 40) return HealthStatus.WARNING;
        if (score <= 70) return HealthStatus.FAIR;
        return HealthStatus.HEALTHY;
    }

    /**
     * Update the health status based on current score.
     */
    public void updateHealthStatus() {
        this.healthStatus = calculateStatus(this.overallHealthScore);
    }
}
