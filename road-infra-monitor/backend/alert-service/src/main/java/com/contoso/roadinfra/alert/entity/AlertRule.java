package com.contoso.roadinfra.alert.entity;

import com.contoso.roadinfra.common.constants.AlertSeverity;
import com.contoso.roadinfra.common.constants.AssetType;
import com.contoso.roadinfra.common.constants.SensorType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Defines rules for automatic alert generation.
 */
@Entity
@Table(name = "alert_rules", indexes = {
        @Index(name = "idx_rule_asset_type", columnList = "asset_type"),
        @Index(name = "idx_rule_sensor_type", columnList = "sensor_type"),
        @Index(name = "idx_rule_enabled", columnList = "enabled")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlertRule {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String code;

    @Column(nullable = false)
    private String name;

    @Column(length = 1000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "asset_type")
    private AssetType assetType;

    @Enumerated(EnumType.STRING)
    @Column(name = "sensor_type")
    private SensorType sensorType;

    @Column(name = "metric_name")
    private String metricName;

    /** Operator: GT, LT, GTE, LTE, EQ, NEQ, BETWEEN, OUTSIDE */
    @Column(nullable = false)
    private String operator;

    /** Primary threshold value */
    @Column(name = "threshold_value")
    private Double thresholdValue;

    /** Secondary threshold value (for BETWEEN/OUTSIDE operators) */
    @Column(name = "threshold_value_secondary")
    private Double thresholdValueSecondary;

    /** Unit of measurement */
    private String unit;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AlertSeverity severity;

    /** Alert title template (supports {sensor}, {asset}, {value} placeholders) */
    @Column(name = "title_template", nullable = false)
    private String titleTemplate;

    /** Alert description template */
    @Column(name = "description_template", length = 2000)
    private String descriptionTemplate;

    /** Category to assign to generated alerts */
    private String category;

    /** Minutes before re-alerting on same condition */
    @Column(name = "cooldown_minutes")
    @Builder.Default
    private Integer cooldownMinutes = 30;

    /** Number of consecutive violations before alerting */
    @Column(name = "consecutive_count")
    @Builder.Default
    private Integer consecutiveCount = 1;

    /** Priority for rule evaluation (lower = higher priority) */
    @Builder.Default
    private Integer priority = 100;

    /** Whether to auto-resolve when condition clears */
    @Column(name = "auto_resolve")
    @Builder.Default
    private Boolean autoResolve = true;

    /** Escalation configuration - minutes before escalating */
    @Column(name = "escalation_minutes")
    private Integer escalationMinutes;

    /** Escalation configuration - target severity after escalation */
    @Enumerated(EnumType.STRING)
    @Column(name = "escalation_severity")
    private AlertSeverity escalationSeverity;

    @Column(nullable = false)
    @Builder.Default
    private Boolean enabled = true;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * Evaluate if a value triggers this rule.
     */
    public boolean evaluate(Double value) {
        if (value == null || thresholdValue == null) {
            return false;
        }
        
        return switch (operator.toUpperCase()) {
            case "GT" -> value > thresholdValue;
            case "LT" -> value < thresholdValue;
            case "GTE" -> value >= thresholdValue;
            case "LTE" -> value <= thresholdValue;
            case "EQ" -> Math.abs(value - thresholdValue) < 0.0001;
            case "NEQ" -> Math.abs(value - thresholdValue) >= 0.0001;
            case "BETWEEN" -> thresholdValueSecondary != null && 
                    value >= thresholdValue && value <= thresholdValueSecondary;
            case "OUTSIDE" -> thresholdValueSecondary != null && 
                    (value < thresholdValue || value > thresholdValueSecondary);
            default -> false;
        };
    }

    /**
     * Generate alert title from template.
     */
    public String generateTitle(String sensorName, String assetName, Double value) {
        return titleTemplate
                .replace("{sensor}", sensorName != null ? sensorName : "Unknown")
                .replace("{asset}", assetName != null ? assetName : "Unknown")
                .replace("{value}", value != null ? String.format("%.2f", value) : "N/A")
                .replace("{threshold}", thresholdValue != null ? String.format("%.2f", thresholdValue) : "N/A")
                .replace("{unit}", unit != null ? unit : "");
    }

    /**
     * Generate alert description from template.
     */
    public String generateDescription(String sensorName, String assetName, Double value) {
        if (descriptionTemplate == null) {
            return String.format("Rule '%s' triggered: %s at %s = %.2f %s (threshold: %.2f %s)",
                    name, sensorName, assetName, value, unit, thresholdValue, unit);
        }
        return descriptionTemplate
                .replace("{sensor}", sensorName != null ? sensorName : "Unknown")
                .replace("{asset}", assetName != null ? assetName : "Unknown")
                .replace("{value}", value != null ? String.format("%.2f", value) : "N/A")
                .replace("{threshold}", thresholdValue != null ? String.format("%.2f", thresholdValue) : "N/A")
                .replace("{unit}", unit != null ? unit : "");
    }
}
