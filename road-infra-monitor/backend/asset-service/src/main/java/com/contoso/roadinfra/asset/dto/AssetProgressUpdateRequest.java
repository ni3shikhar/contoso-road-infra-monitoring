package com.contoso.roadinfra.asset.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to update asset completion percentage")
public class AssetProgressUpdateRequest {

    @NotNull(message = "Completion percentage is required")
    @Min(value = 0, message = "Completion percentage must be at least 0")
    @Max(value = 100, message = "Completion percentage cannot exceed 100")
    @Schema(description = "Completion percentage (0-100)", example = "75.5")
    private Double completionPercentage;

    @Size(max = 500, message = "Notes must be at most 500 characters")
    @Schema(description = "Optional notes about progress update")
    private String notes;
}
