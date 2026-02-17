package com.contoso.roadinfra.sensor.dto;

import com.contoso.roadinfra.common.constants.SensorType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for sensor count grouped by type.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Sensor count by type")
public class SensorCountByTypeResponse {

    @Schema(description = "Sensor type")
    private SensorType sensorType;

    @Schema(description = "Number of sensors of this type")
    private Long count;
}
