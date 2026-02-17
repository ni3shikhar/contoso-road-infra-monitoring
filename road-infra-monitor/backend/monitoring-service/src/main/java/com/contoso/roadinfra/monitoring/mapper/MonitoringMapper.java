package com.contoso.roadinfra.monitoring.mapper;

import com.contoso.roadinfra.monitoring.dto.AssetHealthResponse;
import com.contoso.roadinfra.monitoring.dto.HealthThresholdResponse;
import com.contoso.roadinfra.monitoring.dto.HealthThresholdUpdateRequest;
import com.contoso.roadinfra.monitoring.entity.AssetHealthRecord;
import com.contoso.roadinfra.monitoring.entity.HealthThreshold;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface MonitoringMapper {

    @Mapping(target = "assetName", ignore = true) // Will be populated from asset-service
    AssetHealthResponse toHealthResponse(AssetHealthRecord record);

    List<AssetHealthResponse> toHealthResponseList(List<AssetHealthRecord> records);

    HealthThresholdResponse toThresholdResponse(HealthThreshold threshold);

    List<HealthThresholdResponse> toThresholdResponseList(List<HealthThreshold> thresholds);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "assetType", ignore = true)
    @Mapping(target = "sensorType", ignore = true)
    @Mapping(target = "metricName", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateThreshold(HealthThresholdUpdateRequest request, @MappingTarget HealthThreshold threshold);
}
