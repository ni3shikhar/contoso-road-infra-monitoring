package com.contoso.roadinfra.asset.mapper;

import com.contoso.roadinfra.asset.dto.AssetCreateRequest;
import com.contoso.roadinfra.asset.dto.AssetResponse;
import com.contoso.roadinfra.asset.dto.AssetUpdateRequest;
import com.contoso.roadinfra.asset.entity.Asset;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface AssetMapper {

    @Mapping(target = "inspectionOverdue", expression = "java(asset.isInspectionOverdue())")
    @Mapping(target = "delayed", expression = "java(asset.isDelayed())")
    @Mapping(target = "childAssetCount", ignore = true) // Set by service
    @Mapping(target = "sensorCount", ignore = true) // Set by service via Feign
    @Mapping(target = "children", ignore = true) // Set by service
    AssetResponse toResponse(Asset asset);

    List<AssetResponse> toResponseList(List<Asset> assets);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "inspections", ignore = true)
    @Mapping(target = "milestones", ignore = true)
    @Mapping(target = "lastInspectionDate", ignore = true)
    @Mapping(target = "nextInspectionDate", ignore = true)
    @Mapping(target = "constructionEndDate", ignore = true)
    @Mapping(target = "completionPercentage", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    Asset toEntity(AssetCreateRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "assetCode", ignore = true)
    @Mapping(target = "assetType", ignore = true)
    @Mapping(target = "inspections", ignore = true)
    @Mapping(target = "milestones", ignore = true)
    @Mapping(target = "completionPercentage", ignore = true)
    @Mapping(target = "healthStatus", ignore = true)
    @Mapping(target = "lastInspectionDate", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntity(AssetUpdateRequest request, @MappingTarget Asset entity);
}
