package com.contoso.roadinfra.sensor.entity;

import com.contoso.roadinfra.common.constants.DataQuality;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

/**
 * Represents a telemetry reading from an IoT sensor.
 */
@Entity
@Table(name = "sensor_readings", indexes = {
        @Index(name = "idx_reading_sensor_id", columnList = "sensor_id"),
        @Index(name = "idx_reading_timestamp", columnList = "timestamp"),
        @Index(name = "idx_reading_sensor_timestamp", columnList = "sensor_id, timestamp")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SensorReading {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "sensor_id", nullable = false)
    private UUID sensorId;

    @Column(nullable = false)
    private Instant timestamp;

    @Column(nullable = false)
    private Double value;

    @Column(length = 30)
    private String unit;

    @Column(name = "secondary_value")
    private Double secondaryValue;

    @Column(name = "tertiary_value")
    private Double tertiaryValue;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    @Builder.Default
    private DataQuality quality = DataQuality.GOOD;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "raw_payload", columnDefinition = "jsonb")
    private String rawPayload;

    private Boolean anomaly;

    @Column(name = "anomaly_score")
    private Double anomalyScore;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;
}
