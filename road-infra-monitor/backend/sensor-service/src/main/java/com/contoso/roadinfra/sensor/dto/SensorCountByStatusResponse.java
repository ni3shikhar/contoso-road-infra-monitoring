package com.contoso.roadinfra.sensor.dto;

import com.contoso.roadinfra.common.constants.SensorStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for sensor count grouped by status.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Sensor count by status")
public class SensorCountByStatusResponse {

    @Schema(description = "Sensor status")
    private SensorStatus status;

    @Schema(description = "Number of sensors with this status")
    private Long count;
}
