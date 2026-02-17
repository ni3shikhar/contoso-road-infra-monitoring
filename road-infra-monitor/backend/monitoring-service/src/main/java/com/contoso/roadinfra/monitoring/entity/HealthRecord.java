package com.contoso.roadinfra.monitoring.entity;

import com.contoso.roadinfra.common.constants.HealthStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "health_records", indexes = {
        @Index(name = "idx_health_asset_id", columnList = "asset_id"),
        @Index(name = "idx_health_sensor_id", columnList = "sensor_id"),
        @Index(name = "idx_health_checked_at", columnList = "checked_at")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HealthRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "asset_id")
    private UUID assetId;

    @Column(name = "asset_name")
    private String assetName;

    @Column(name = "sensor_id")
    private UUID sensorId;

    @Column(name = "sensor_name")
    private String sensorName;

    @Enumerated(EnumType.STRING)
    private HealthStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "previous_status")
    private HealthStatus previousStatus;

    @Column(name = "health_score")
    private Integer healthScore;

    @ElementCollection
    @CollectionTable(name = "health_record_metrics", joinColumns = @JoinColumn(name = "health_record_id"))
    @MapKeyColumn(name = "metric_name")
    @Column(name = "metric_value")
    @Builder.Default
    private Map<String, Double> metrics = new HashMap<>();

    @Column(name = "status_changed")
    private Boolean statusChanged;

    @Column(name = "status_duration_minutes")
    private Long statusDurationMinutes;

    private String trend;

    @Column(name = "checked_at")
    private LocalDateTime checkedAt;

    @Column(name = "next_check_at")
    private LocalDateTime nextCheckAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
