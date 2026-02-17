package com.contoso.roadinfra.asset.mapper;

import com.contoso.roadinfra.asset.dto.InspectionCreateRequest;
import com.contoso.roadinfra.asset.dto.InspectionResponse;
import com.contoso.roadinfra.asset.entity.AssetInspection;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface InspectionMapper {

    @Mapping(target = "assetId", source = "asset.id")
    @Mapping(target = "assetCode", source = "asset.assetCode")
    @Mapping(target = "assetName", source = "asset.name")
    @Mapping(target = "conditionDescription", expression = "java(inspection.getConditionDescription())")
    @Mapping(target = "critical", expression = "java(inspection.isCritical())")
    InspectionResponse toResponse(AssetInspection inspection);

    List<InspectionResponse> toResponseList(List<AssetInspection> inspections);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "asset", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    AssetInspection toEntity(InspectionCreateRequest request);
}
