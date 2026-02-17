package com.contoso.roadinfra.sensor.dto;

import com.contoso.roadinfra.common.constants.DataQuality;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Request DTO for ingesting a sensor reading.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to ingest a sensor reading")
public class SensorReadingRequest {

    @Schema(description = "Timestamp of the reading (defaults to now if not provided)")
    private Instant timestamp;

    @NotNull(message = "Value is required")
    @Schema(description = "Primary measurement value", example = "125.5")
    private Double value;

    @Size(max = 30, message = "Unit must not exceed 30 characters")
    @Schema(description = "Unit of measurement", example = "microstrain")
    private String unit;

    @Schema(description = "Secondary value for multi-axis sensors", example = "45.2")
    private Double secondaryValue;

    @Schema(description = "Tertiary value for multi-axis sensors", example = "12.8")
    private Double tertiaryValue;

    @Schema(description = "Data quality indicator")
    private DataQuality quality;

    @Size(max = 10000, message = "Raw payload must not exceed 10000 characters")
    @Schema(description = "Original sensor transmission as JSON")
    private String rawPayload;
}
