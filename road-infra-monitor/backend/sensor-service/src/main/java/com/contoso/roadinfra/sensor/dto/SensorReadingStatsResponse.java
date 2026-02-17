package com.contoso.roadinfra.sensor.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Response DTO for aggregated sensor reading statistics.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Aggregated statistics for sensor readings")
public class SensorReadingStatsResponse {

    @Schema(description = "Sensor ID")
    private UUID sensorId;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSZ", timezone = "UTC")
    @Schema(description = "Start of the statistics period")
    private Instant periodStart;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSZ", timezone = "UTC")
    @Schema(description = "End of the statistics period")
    private Instant periodEnd;

    @Schema(description = "Total number of readings in the period")
    private Long readingCount;

    @Schema(description = "Minimum value in the period")
    private Double minValue;

    @Schema(description = "Maximum value in the period")
    private Double maxValue;

    @Schema(description = "Average value in the period")
    private Double avgValue;

    @Schema(description = "Standard deviation of values")
    private Double stdDeviation;

    @Schema(description = "Number of anomalous readings")
    private Long anomalyCount;

    @Schema(description = "Unit of measurement")
    private String unit;
}
