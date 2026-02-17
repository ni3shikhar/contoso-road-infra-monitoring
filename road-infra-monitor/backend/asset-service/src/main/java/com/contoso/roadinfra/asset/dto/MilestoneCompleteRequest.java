package com.contoso.roadinfra.asset.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.PastOrPresent;
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
@Schema(description = "Request to mark a milestone as complete")
public class MilestoneCompleteRequest {

    @PastOrPresent(message = "Completion date cannot be in the future")
    @Schema(description = "Actual completion date (defaults to today if not provided)")
    private LocalDate completionDate;

    @Size(max = 2000, message = "Notes must be at most 2000 characters")
    @Schema(description = "Completion notes")
    private String notes;
}
