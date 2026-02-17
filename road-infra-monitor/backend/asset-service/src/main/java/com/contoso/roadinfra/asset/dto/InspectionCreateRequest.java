package com.contoso.roadinfra.asset.dto;

import com.contoso.roadinfra.asset.constants.InspectionType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to record a new inspection")
public class InspectionCreateRequest {

    @NotNull(message = "Inspection date is required")
    @PastOrPresent(message = "Inspection date cannot be in the future")
    @Schema(description = "Date of inspection")
    private LocalDate inspectionDate;

    @NotBlank(message = "Inspector name is required")
    @Size(max = 200, message = "Inspector name must be at most 200 characters")
    @Schema(description = "Name of inspector", example = "John Smith")
    private String inspectorName;

    @NotNull(message = "Inspection type is required")
    @Schema(description = "Type of inspection")
    private InspectionType inspectionType;

    @NotNull(message = "Overall condition rating is required")
    @Min(value = 1, message = "Rating must be between 1 and 5")
    @Max(value = 5, message = "Rating must be between 1 and 5")
    @Schema(description = "Overall condition rating (1=Critical, 5=Excellent)", example = "4")
    private Integer overallConditionRating;

    @Size(max = 4000, message = "Findings must be at most 4000 characters")
    @Schema(description = "Inspection findings")
    private String findings;

    @Size(max = 4000, message = "Recommendations must be at most 4000 characters")
    @Schema(description = "Recommendations based on inspection")
    private String recommendations;

    @Future(message = "Next inspection date must be in the future")
    @Schema(description = "Recommended date for next inspection")
    private LocalDate nextInspectionRecommendedDate;

    @Size(max = 20, message = "Maximum 20 photos allowed")
    @Schema(description = "URLs to inspection photos")
    private List<String> photos;
}
