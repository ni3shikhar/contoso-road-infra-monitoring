package com.contoso.roadinfra.asset.dto;

import com.contoso.roadinfra.asset.constants.MilestoneStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to update a milestone")
public class MilestoneUpdateRequest {

    @Size(max = 200, message = "Milestone name must be at most 200 characters")
    @Schema(description = "Milestone name")
    private String milestoneName;

    @Schema(description = "Planned completion date")
    private LocalDate plannedDate;

    @Schema(description = "Actual completion date")
    private LocalDate actualDate;

    @Schema(description = "Milestone status")
    private MilestoneStatus status;

    @Min(value = 1, message = "Sequence order must be at least 1")
    @Schema(description = "Order in construction sequence")
    private Integer sequenceOrder;

    @Size(max = 2000, message = "Notes must be at most 2000 characters")
    @Schema(description = "Additional notes")
    private String notes;
}
