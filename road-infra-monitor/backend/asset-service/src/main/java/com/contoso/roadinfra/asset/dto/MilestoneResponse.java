package com.contoso.roadinfra.asset.dto;

import com.contoso.roadinfra.asset.constants.MilestoneStatus;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Milestone response")
public class MilestoneResponse {

    @Schema(description = "Milestone ID")
    private UUID id;

    @Schema(description = "Asset ID")
    private UUID assetId;

    @Schema(description = "Asset code")
    private String assetCode;

    @Schema(description = "Asset name")
    private String assetName;

    @Schema(description = "Milestone name")
    private String milestoneName;

    @JsonFormat(pattern = "yyyy-MM-dd")
    @Schema(description = "Planned date")
    private LocalDate plannedDate;

    @JsonFormat(pattern = "yyyy-MM-dd")
    @Schema(description = "Actual date")
    private LocalDate actualDate;

    @Schema(description = "Milestone status")
    private MilestoneStatus status;

    @Schema(description = "Delay in days (positive = delayed, negative = ahead)")
    private Integer delayDays;

    @Schema(description = "Additional notes")
    private String notes;

    @Schema(description = "Sequence order")
    private Integer sequenceOrder;

    @Schema(description = "Whether milestone is currently delayed")
    private boolean delayed;

    @Schema(description = "Created by user")
    private String createdBy;

    @Schema(description = "Last updated by user")
    private String updatedBy;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
    @Schema(description = "Created timestamp")
    private Instant createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
    @Schema(description = "Updated timestamp")
    private Instant updatedAt;
}
