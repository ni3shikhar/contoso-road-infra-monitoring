package com.contoso.roadinfra.common.dto;

import com.contoso.roadinfra.common.constants.AssetType;
import com.contoso.roadinfra.common.constants.HealthStatus;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Asset data transfer object")
public class AssetDTO {

    @Schema(description = "Unique asset identifier", example = "550e8400-e29b-41d4-a716-446655440001")
    private UUID id;

    @NotBlank(message = "Asset name is required")
    @Schema(description = "Asset name", example = "Golden Gate Bridge Section A")
    private String name;

    @NotNull(message = "Asset type is required")
    @Schema(description = "Type of infrastructure asset")
    private AssetType assetType;

    @Schema(description = "Detailed description of the asset")
    private String description;

    @Schema(description = "Geographic latitude of asset center", example = "37.8199")
    private Double latitude;

    @Schema(description = "Geographic longitude of asset center", example = "-122.4783")
    private Double longitude;

    @Schema(description = "GeoJSON representation of asset boundaries")
    private String geoJson;

    @Schema(description = "Physical address or location")
    private String address;

    @Schema(description = "City")
    private String city;

    @Schema(description = "State or province")
    private String state;

    @Schema(description = "Country")
    private String country;

    @Schema(description = "Postal code")
    private String postalCode;

    @Schema(description = "Current overall health status")
    private HealthStatus healthStatus;

    @Schema(description = "Health score from 0-100", example = "85")
    private Integer healthScore;

    @JsonFormat(pattern = "yyyy-MM-dd")
    @Schema(description = "Construction completion date")
    private LocalDate constructionDate;

    @JsonFormat(pattern = "yyyy-MM-dd")
    @Schema(description = "Last inspection date")
    private LocalDate lastInspectionDate;

    @JsonFormat(pattern = "yyyy-MM-dd")
    @Schema(description = "Next scheduled inspection date")
    private LocalDate nextInspectionDate;

    @Schema(description = "Expected lifespan in years", example = "75")
    private Integer expectedLifespanYears;

    @Schema(description = "Current age in years", example = "25")
    private Integer currentAgeYears;

    @Schema(description = "Total number of sensors attached")
    private Integer sensorCount;

    @Schema(description = "Number of active alerts")
    private Integer activeAlertCount;

    @Schema(description = "IDs of associated sensors")
    private List<UUID> sensorIds;

    @Schema(description = "Tags for categorization")
    private List<String> tags;

    @Schema(description = "Whether the asset is actively monitored")
    private Boolean active;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Schema(description = "Record creation timestamp")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Schema(description = "Record last update timestamp")
    private LocalDateTime updatedAt;
}
