package com.contoso.roadinfra.sensor.mapper;

import com.contoso.roadinfra.sensor.dto.SensorReadingRequest;
import com.contoso.roadinfra.sensor.dto.SensorReadingResponse;
import com.contoso.roadinfra.sensor.entity.SensorReading;
import org.mapstruct.*;

import java.util.List;
import java.util.UUID;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface SensorReadingMapper {

    SensorReadingResponse toResponse(SensorReading reading);

    List<SensorReadingResponse> toResponseList(List<SensorReading> readings);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "sensorId", source = "sensorId")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "anomaly", ignore = true)
    @Mapping(target = "anomalyScore", ignore = true)
    SensorReading toEntity(SensorReadingRequest request, UUID sensorId);

    default SensorReading toEntityWithDefaults(SensorReadingRequest request, UUID sensorId) {
        SensorReading reading = toEntity(request, sensorId);
        if (reading.getTimestamp() == null) {
            reading.setTimestamp(java.time.Instant.now());
        }
        if (reading.getQuality() == null) {
            reading.setQuality(com.contoso.roadinfra.common.constants.DataQuality.GOOD);
        }
        return reading;
    }
}
