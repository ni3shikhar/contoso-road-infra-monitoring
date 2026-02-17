package com.contoso.roadinfra.alert.mapper;

import com.contoso.roadinfra.alert.entity.Alert;
import com.contoso.roadinfra.common.dto.AlertDTO;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface AlertMapper {

    AlertDTO toDto(Alert alert);

    List<AlertDTO> toDtoList(List<Alert> alerts);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Alert toEntity(AlertDTO dto);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntity(AlertDTO dto, @MappingTarget Alert entity);
}
