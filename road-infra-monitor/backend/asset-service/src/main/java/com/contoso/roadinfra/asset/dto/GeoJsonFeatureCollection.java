package com.contoso.roadinfra.asset.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "GeoJSON Feature Collection for mapping assets")
public class GeoJsonFeatureCollection {

    @Schema(description = "GeoJSON type", example = "FeatureCollection")
    @Builder.Default
    private String type = "FeatureCollection";

    @Schema(description = "Features array")
    private List<Feature> features;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "GeoJSON Feature")
    public static class Feature {

        @Schema(description = "Feature type", example = "Feature")
        @Builder.Default
        private String type = "Feature";

        @Schema(description = "Feature ID")
        private String id;

        @Schema(description = "Geometry object")
        private Geometry geometry;

        @Schema(description = "Feature properties")
        private Map<String, Object> properties;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "GeoJSON Geometry")
    public static class Geometry {

        @Schema(description = "Geometry type (Point, LineString, Polygon)", example = "LineString")
        private String type;

        @Schema(description = "Coordinates array")
        private Object coordinates;
    }
}
