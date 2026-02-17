package com.contoso.roadinfra.simulator.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Request model for posting sensor readings.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SensorReadingRequest {
    private Instant timestamp;
    private Double value;
    private String unit;
    private Double secondaryValue;
    private Double tertiaryValue;
    private String quality;
    private String rawPayload;
}
