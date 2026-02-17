package com.contoso.roadinfra.asset.dto;

import com.contoso.roadinfra.asset.constants.InspectionType;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Inspection response")
public class InspectionResponse {

    @Schema(description = "Inspection ID")
    private UUID id;

    @Schema(description = "Asset ID")
    private UUID assetId;

    @Schema(description = "Asset code")
    private String assetCode;

    @Schema(description = "Asset name")
    private String assetName;

    @JsonFormat(pattern = "yyyy-MM-dd")
    @Schema(description = "Inspection date")
    private LocalDate inspectionDate;

    @Schema(description = "Inspector name")
    private String inspectorName;

    @Schema(description = "Inspection type")
    private InspectionType inspectionType;

    @Schema(description = "Overall condition rating (1-5)")
    private Integer overallConditionRating;

    @Schema(description = "Condition description based on rating")
    private String conditionDescription;

    @Schema(description = "Inspection findings")
    private String findings;

    @Schema(description = "Recommendations")
    private String recommendations;

    @JsonFormat(pattern = "yyyy-MM-dd")
    @Schema(description = "Recommended next inspection date")
    private LocalDate nextInspectionRecommendedDate;

    @Schema(description = "Photo URLs")
    private List<String> photos;

    @Schema(description = "Whether condition is critical (rating <= 2)")
    private boolean critical;

    @Schema(description = "Created by user")
    private String createdBy;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
    @Schema(description = "Created timestamp")
    private Instant createdAt;
}
