package com.contoso.roadinfra.simulator.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Response model for asset data from the asset-service API.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class AssetResponse {
    private UUID id;
    private String assetCode;
    private String name;
    private String assetType;
    private String status;
    private Double latitude;
    private Double longitude;
    private String description;
    private UUID parentId;
}
