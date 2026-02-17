package com.contoso.roadinfra.asset.mapper;

import com.contoso.roadinfra.asset.dto.MilestoneCreateRequest;
import com.contoso.roadinfra.asset.dto.MilestoneResponse;
import com.contoso.roadinfra.asset.dto.MilestoneUpdateRequest;
import com.contoso.roadinfra.asset.entity.ConstructionMilestone;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface MilestoneMapper {

    @Mapping(target = "assetId", source = "asset.id")
    @Mapping(target = "assetCode", source = "asset.assetCode")
    @Mapping(target = "assetName", source = "asset.name")
    @Mapping(target = "delayed", expression = "java(milestone.isDelayed())")
    @Mapping(target = "delayDays", expression = "java(milestone.calculateDelayDays())")
    MilestoneResponse toResponse(ConstructionMilestone milestone);

    List<MilestoneResponse> toResponseList(List<ConstructionMilestone> milestones);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "asset", ignore = true)
    @Mapping(target = "actualCompletionDate", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    ConstructionMilestone toEntity(MilestoneCreateRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "asset", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntity(MilestoneUpdateRequest request, @MappingTarget ConstructionMilestone entity);
}
