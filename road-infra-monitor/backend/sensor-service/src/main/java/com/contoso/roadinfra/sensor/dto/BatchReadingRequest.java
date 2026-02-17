package com.contoso.roadinfra.sensor.dto;

import com.contoso.roadinfra.common.constants.DataQuality;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Request DTO for batch ingestion of sensor readings.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to batch ingest sensor readings")
public class BatchReadingRequest {

    @NotEmpty(message = "At least one reading is required")
    @Size(max = 1000, message = "Cannot batch more than 1000 readings at once")
    @Valid
    @Schema(description = "List of readings to ingest")
    private List<BatchReadingItem> readings;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Individual reading item in a batch")
    public static class BatchReadingItem {

        @NotNull(message = "Sensor ID is required")
        @Schema(description = "Sensor ID")
        private UUID sensorId;

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
}
