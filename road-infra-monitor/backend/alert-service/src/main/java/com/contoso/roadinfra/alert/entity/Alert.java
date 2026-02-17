package com.contoso.roadinfra.alert.entity;

import com.contoso.roadinfra.alert.constants.AlertStatus;
import com.contoso.roadinfra.alert.constants.SourceType;
import com.contoso.roadinfra.common.constants.AlertSeverity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "alerts", indexes = {
        @Index(name = "idx_alert_asset_id", columnList = "asset_id"),
        @Index(name = "idx_alert_sensor_id", columnList = "sensor_id"),
        @Index(name = "idx_alert_severity", columnList = "severity"),
        @Index(name = "idx_alert_status", columnList = "alert_status"),
        @Index(name = "idx_alert_code", columnList = "alert_code"),
        @Index(name = "idx_alert_source_type", columnList = "source_type")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Alert {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** Unique code for alert type (e.g., "SENSOR_THRESHOLD_EXCEEDED") */
    @Column(name = "alert_code")
    private String alertCode;

    /** Reference to the rule that generated this alert */
    @Column(name = "rule_id")
    private UUID ruleId;

    @Column(nullable = false)
    private String title;

    @Column(length = 2000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AlertSeverity severity;

    /** Original severity before any escalation */
    @Enumerated(EnumType.STRING)
    @Column(name = "original_severity")
    private AlertSeverity originalSeverity;

    /** Source type of the alert */
    @Enumerated(EnumType.STRING)
    @Column(name = "source_type")
    @Builder.Default
    private SourceType sourceType = SourceType.SENSOR;

    /** Current escalation level (0 = not escalated) */
    @Column(name = "escalation_level")
    @Builder.Default
    private Integer escalationLevel = 0;

    /** Time when last escalation occurred */
    @Column(name = "escalated_at")
    private LocalDateTime escalatedAt;

    private String category;

    @Column(name = "asset_id")
    private UUID assetId;

    @Column(name = "asset_name")
    private String assetName;

    @Column(name = "sensor_id")
    private UUID sensorId;

    @Column(name = "sensor_name")
    private String sensorName;

    @Column(name = "trigger_value")
    private Double triggerValue;

    @Column(name = "threshold_value")
    private Double thresholdValue;

    private String unit;

    @Enumerated(EnumType.STRING)
    @Column(name = "alert_status", nullable = false)
    @Builder.Default
    private AlertStatus alertStatus = AlertStatus.OPEN;

    /** Legacy status field for backward compatibility */
    @Deprecated
    @Builder.Default
    private String status = "OPEN";

    @Builder.Default
    private Boolean acknowledged = false;

    @Column(name = "acknowledged_by")
    private UUID acknowledgedBy;

    @Column(name = "acknowledged_by_name")
    private String acknowledgedByName;

    @Column(name = "acknowledged_at")
    private LocalDateTime acknowledgedAt;

    @Builder.Default
    private Boolean resolved = false;

    @Column(name = "resolved_by")
    private UUID resolvedBy;

    @Column(name = "resolved_by_name")
    private String resolvedByName;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @Column(name = "resolution_notes", length = 2000)
    private String resolutionNotes;

    @ElementCollection
    @CollectionTable(name = "alert_notification_channels", joinColumns = @JoinColumn(name = "alert_id"))
    @Column(name = "channel")
    @Builder.Default
    private List<String> notificationChannels = new ArrayList<>();

    @Column(name = "notifications_sent")
    @Builder.Default
    private Boolean notificationsSent = false;

    @ElementCollection
    @CollectionTable(name = "alert_tags", joinColumns = @JoinColumn(name = "alert_id"))
    @Column(name = "tag")
    @Builder.Default
    private List<String> tags = new ArrayList<>();

    @Column(name = "triggered_at")
    private LocalDateTime triggeredAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
