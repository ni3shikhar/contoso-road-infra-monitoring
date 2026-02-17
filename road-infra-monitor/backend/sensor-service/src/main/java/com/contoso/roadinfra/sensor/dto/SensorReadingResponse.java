package com.contoso.roadinfra.sensor.dto;

import com.contoso.roadinfra.common.constants.DataQuality;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Response DTO for sensor reading data.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Sensor reading response data")
public class SensorReadingResponse {

    @Schema(description = "Unique reading identifier")
    private UUID id;

    @Schema(description = "Sensor ID")
    private UUID sensorId;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSZ", timezone = "UTC")
    @Schema(description = "Timestamp of the reading")
    private Instant timestamp;

    @Schema(description = "Primary measurement value")
    private Double value;

    @Schema(description = "Unit of measurement")
    private String unit;

    @Schema(description = "Secondary value for multi-axis sensors")
    private Double secondaryValue;

    @Schema(description = "Tertiary value for multi-axis sensors")
    private Double tertiaryValue;

    @Schema(description = "Data quality indicator")
    private DataQuality quality;

    @Schema(description = "Original sensor transmission as JSON")
    private String rawPayload;

    @Schema(description = "Whether this reading was flagged as anomalous")
    private Boolean anomaly;

    @Schema(description = "Anomaly score if applicable")
    private Double anomalyScore;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSZ", timezone = "UTC")
    @Schema(description = "Record creation timestamp")
    private Instant createdAt;
}
