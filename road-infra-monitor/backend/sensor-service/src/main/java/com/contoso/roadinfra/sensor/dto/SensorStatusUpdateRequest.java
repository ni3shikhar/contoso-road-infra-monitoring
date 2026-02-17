package com.contoso.roadinfra.sensor.dto;

import com.contoso.roadinfra.common.constants.SensorStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for updating sensor status.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to update sensor status")
public class SensorStatusUpdateRequest {

    @NotNull(message = "Status is required")
    @Schema(description = "New sensor status")
    private SensorStatus status;

    @Schema(description = "Reason for status change", example = "Scheduled maintenance completed")
    private String reason;
}
