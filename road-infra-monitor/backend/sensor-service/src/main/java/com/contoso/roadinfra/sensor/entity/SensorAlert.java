package com.contoso.roadinfra.sensor.entity;

import com.contoso.roadinfra.common.constants.AlertSeverity;
import com.contoso.roadinfra.common.constants.SensorAlertType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

/**
 * Represents an alert generated for a sensor condition.
 */
@Entity
@Table(name = "sensor_alerts", indexes = {
        @Index(name = "idx_alert_sensor_id", columnList = "sensor_id"),
        @Index(name = "idx_alert_type", columnList = "alert_type"),
        @Index(name = "idx_alert_severity", columnList = "severity"),
        @Index(name = "idx_alert_acknowledged", columnList = "acknowledged"),
        @Index(name = "idx_alert_created_at", columnList = "created_at")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SensorAlert {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "sensor_id", nullable = false)
    private UUID sensorId;

    @Enumerated(EnumType.STRING)
    @Column(name = "alert_type", nullable = false, length = 30)
    private SensorAlertType alertType;

    @Column(nullable = false, length = 500)
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AlertSeverity severity;

    @Column(nullable = false)
    @Builder.Default
    private Boolean acknowledged = false;

    @Column(name = "acknowledged_by", length = 100)
    private String acknowledgedBy;

    @Column(name = "acknowledged_at")
    private Instant acknowledgedAt;

    @Column(name = "reading_value")
    private Double readingValue;

    @Column(name = "threshold_value")
    private Double thresholdValue;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false, nullable = false)
    private Instant createdAt;

    /**
     * Acknowledge this alert.
     */
    public void acknowledge(String username) {
        this.acknowledged = true;
        this.acknowledgedBy = username;
        this.acknowledgedAt = Instant.now();
    }
}
