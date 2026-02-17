package com.contoso.roadinfra.sensor.mapper;

import com.contoso.roadinfra.sensor.dto.SensorCreateRequest;
import com.contoso.roadinfra.sensor.dto.SensorResponse;
import com.contoso.roadinfra.sensor.dto.SensorUpdateRequest;
import com.contoso.roadinfra.sensor.entity.Sensor;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface SensorMapper {

    @Mapping(target = "calibrationDue", expression = "java(sensor.isCalibrationDue())")
    SensorResponse toResponse(Sensor sensor);

    List<SensorResponse> toResponseList(List<Sensor> sensors);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "currentValue", ignore = true)
    @Mapping(target = "lastDataReceivedAt", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    Sensor toEntity(SensorCreateRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "sensorCode", ignore = true)
    @Mapping(target = "sensorType", ignore = true)
    @Mapping(target = "installationDate", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "currentValue", ignore = true)
    @Mapping(target = "lastDataReceivedAt", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntity(SensorUpdateRequest request, @MappingTarget Sensor entity);
}
