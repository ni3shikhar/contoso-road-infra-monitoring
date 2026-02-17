package com.contoso.roadinfra.sensor.mapper;

import com.contoso.roadinfra.sensor.dto.SensorAlertResponse;
import com.contoso.roadinfra.sensor.entity.Sensor;
import com.contoso.roadinfra.sensor.entity.SensorAlert;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface SensorAlertMapper {

    @Mapping(target = "sensorCode", ignore = true)
    SensorAlertResponse toResponse(SensorAlert alert);

    List<SensorAlertResponse> toResponseList(List<SensorAlert> alerts);

    /**
     * Map alert to response with sensor code included.
     */
    default SensorAlertResponse toResponseWithSensorCode(SensorAlert alert, Sensor sensor) {
        SensorAlertResponse response = toResponse(alert);
        if (sensor != null) {
            response.setSensorCode(sensor.getSensorCode());
        }
        return response;
    }
}
