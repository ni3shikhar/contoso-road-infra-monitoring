package com.contoso.roadinfra.sensor.entity;

import com.contoso.roadinfra.common.constants.AssetType;
import com.contoso.roadinfra.common.constants.SensorStatus;
import com.contoso.roadinfra.common.constants.SensorType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Represents an IoT sensor deployed on the 2km road corridor including bridge and tunnel sections.
 */
@Entity
@Table(name = "sensors", indexes = {
        @Index(name = "idx_sensor_asset_id", columnList = "asset_id"),
        @Index(name = "idx_sensor_type", columnList = "sensor_type"),
        @Index(name = "idx_sensor_status", columnList = "status"),
        @Index(name = "idx_sensor_code", columnList = "sensor_code", unique = true)
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Sensor {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "sensor_code", nullable = false, unique = true, length = 50)
    private String sensorCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "sensor_type", nullable = false, length = 30)
    private SensorType sensorType;

    @Column(nullable = false, length = 100)
    private String manufacturer;

    @Column(nullable = false, length = 100)
    private String model;

    @Column(name = "installation_date", nullable = false)
    private LocalDate installationDate;

    @Column(name = "last_calibration_date")
    private LocalDate lastCalibrationDate;

    @Column(name = "calibration_interval_days")
    private Integer calibrationIntervalDays;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private SensorStatus status = SensorStatus.INACTIVE;

    @Column(nullable = false)
    private Double latitude;

    @Column(nullable = false)
    private Double longitude;

    private Double elevation;

    @Column(name = "asset_id", nullable = false)
    private UUID assetId;

    @Enumerated(EnumType.STRING)
    @Column(name = "asset_type", nullable = false, length = 20)
    private AssetType assetType;

    @Column(name = "location_description", length = 255)
    private String locationDescription;

    @Column(name = "battery_level")
    private Double batteryLevel;

    @Column(name = "signal_strength")
    private Double signalStrength;

    @Column(name = "firmware_version", length = 50)
    private String firmwareVersion;

    @Column(name = "min_threshold")
    private Double minThreshold;

    @Column(name = "max_threshold")
    private Double maxThreshold;

    private String unit;

    @Column(name = "current_value")
    private Double currentValue;

    @Column(name = "last_data_received_at")
    private Instant lastDataReceivedAt;

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
     * Check if calibration is due based on last calibration date and interval.
     */
    public boolean isCalibrationDue() {
        if (lastCalibrationDate == null || calibrationIntervalDays == null) {
            return false;
        }
        return LocalDate.now().isAfter(lastCalibrationDate.plusDays(calibrationIntervalDays));
    }

    /**
     * Check if the sensor has been offline (no data) for a given duration in minutes.
     */
    public boolean isOffline(long thresholdMinutes) {
        if (lastDataReceivedAt == null) {
            return true;
        }
        return Instant.now().minusSeconds(thresholdMinutes * 60).isAfter(lastDataReceivedAt);
    }
}
