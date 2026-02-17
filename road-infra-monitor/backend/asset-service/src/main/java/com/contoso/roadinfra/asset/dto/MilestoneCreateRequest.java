package com.contoso.roadinfra.asset.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to create a construction milestone")
public class MilestoneCreateRequest {

    @NotBlank(message = "Milestone name is required")
    @Size(max = 200, message = "Milestone name must be at most 200 characters")
    @Schema(description = "Milestone name", example = "Foundation Complete")
    private String milestoneName;

    @NotNull(message = "Planned date is required")
    @Schema(description = "Planned completion date")
    private LocalDate plannedDate;

    @NotNull(message = "Sequence order is required")
    @Min(value = 1, message = "Sequence order must be at least 1")
    @Schema(description = "Order in construction sequence", example = "1")
    private Integer sequenceOrder;

    @Size(max = 2000, message = "Notes must be at most 2000 characters")
    @Schema(description = "Additional notes")
    private String notes;
}
