package com.contoso.roadinfra.common.dto;

import com.contoso.roadinfra.common.constants.AlertSeverity;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Alert data transfer object")
public class AlertDTO {

    @Schema(description = "Unique alert identifier")
    private UUID id;

    @NotBlank(message = "Alert title is required")
    @Schema(description = "Alert title", example = "High strain detected on Bridge-01")
    private String title;

    @Schema(description = "Detailed alert description")
    private String description;

    @NotNull(message = "Alert severity is required")
    @Schema(description = "Severity level of the alert")
    private AlertSeverity severity;

    @Schema(description = "Alert category", example = "THRESHOLD_BREACH")
    private String category;

    @Schema(description = "ID of the asset that triggered the alert")
    private UUID assetId;

    @Schema(description = "Name of the asset")
    private String assetName;

    @Schema(description = "ID of the sensor that triggered the alert")
    private UUID sensorId;

    @Schema(description = "Name of the sensor")
    private String sensorName;

    @Schema(description = "Value that triggered the alert")
    private Double triggerValue;

    @Schema(description = "Threshold that was exceeded")
    private Double thresholdValue;

    @Schema(description = "Unit of measurement")
    private String unit;

    @Schema(description = "Current alert status", example = "OPEN")
    private String status;

    @Schema(description = "Whether the alert has been acknowledged")
    private Boolean acknowledged;

    @Schema(description = "ID of user who acknowledged")
    private UUID acknowledgedBy;

    @Schema(description = "Username who acknowledged")
    private String acknowledgedByName;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Schema(description = "When the alert was acknowledged")
    private LocalDateTime acknowledgedAt;

    @Schema(description = "Whether the alert has been resolved")
    private Boolean resolved;

    @Schema(description = "ID of user who resolved")
    private UUID resolvedBy;

    @Schema(description = "Username who resolved")
    private String resolvedByName;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Schema(description = "When the alert was resolved")
    private LocalDateTime resolvedAt;

    @Schema(description = "Resolution notes")
    private String resolutionNotes;

    @Schema(description = "List of notification channels used")
    private List<String> notificationChannels;

    @Schema(description = "Whether notifications have been sent")
    private Boolean notificationsSent;

    @Schema(description = "Related alert IDs")
    private List<UUID> relatedAlertIds;

    @Schema(description = "Tags for categorization")
    private List<String> tags;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Schema(description = "When the alert was triggered")
    private LocalDateTime triggeredAt;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Schema(description = "Record creation timestamp")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Schema(description = "Record last update timestamp")
    private LocalDateTime updatedAt;
}
