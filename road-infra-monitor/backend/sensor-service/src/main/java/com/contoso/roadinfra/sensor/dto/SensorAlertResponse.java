package com.contoso.roadinfra.sensor.dto;

import com.contoso.roadinfra.common.constants.AlertSeverity;
import com.contoso.roadinfra.common.constants.SensorAlertType;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Response DTO for sensor alert data.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Sensor alert response data")
public class SensorAlertResponse {

    @Schema(description = "Unique alert identifier")
    private UUID id;

    @Schema(description = "Sensor ID")
    private UUID sensorId;

    @Schema(description = "Sensor code for reference")
    private String sensorCode;

    @Schema(description = "Type of alert")
    private SensorAlertType alertType;

    @Schema(description = "Alert message")
    private String message;

    @Schema(description = "Alert severity")
    private AlertSeverity severity;

    @Schema(description = "Whether the alert has been acknowledged")
    private Boolean acknowledged;

    @Schema(description = "User who acknowledged the alert")
    private String acknowledgedBy;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSZ", timezone = "UTC")
    @Schema(description = "When the alert was acknowledged")
    private Instant acknowledgedAt;

    @Schema(description = "Reading value that triggered the alert")
    private Double readingValue;

    @Schema(description = "Threshold value that was breached")
    private Double thresholdValue;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSZ", timezone = "UTC")
    @Schema(description = "When the alert was created")
    private Instant createdAt;
}
