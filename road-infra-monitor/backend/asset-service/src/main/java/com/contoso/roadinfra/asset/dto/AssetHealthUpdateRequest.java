package com.contoso.roadinfra.asset.dto;

import com.contoso.roadinfra.common.constants.HealthStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to update asset health status")
public class AssetHealthUpdateRequest {

    @NotNull(message = "Health status is required")
    @Schema(description = "New health status")
    private HealthStatus healthStatus;

    @Size(max = 500, message = "Reason must be at most 500 characters")
    @Schema(description = "Reason for health status change")
    private String reason;
}
