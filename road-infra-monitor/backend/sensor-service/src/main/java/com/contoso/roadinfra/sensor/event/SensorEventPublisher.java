package com.contoso.roadinfra.sensor.event;

import com.contoso.roadinfra.common.constants.SensorStatus;
import com.contoso.roadinfra.sensor.dto.SensorAlertResponse;
import com.contoso.roadinfra.sensor.dto.SensorReadingResponse;
import com.contoso.roadinfra.sensor.dto.SensorResponse;
import com.contoso.roadinfra.sensor.entity.Sensor;
import com.contoso.roadinfra.sensor.entity.SensorAlert;
import com.contoso.roadinfra.sensor.entity.SensorReading;
import com.contoso.roadinfra.sensor.mapper.SensorAlertMapper;
import com.contoso.roadinfra.sensor.mapper.SensorMapper;
import com.contoso.roadinfra.sensor.mapper.SensorReadingMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Publishes sensor events to Kafka topics and WebSocket channels.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SensorEventPublisher {

    private static final String TOPIC_SENSOR_ALERTS = "sensor-alerts";
    private static final String TOPIC_SENSOR_STATUS_CHANGES = "sensor-status-changes";
    private static final String TOPIC_SENSOR_TELEMETRY = "sensor-telemetry";
    private static final String TOPIC_SENSOR_EVENTS = "sensor-events";

    private static final String WS_TOPIC_READINGS_ALL = "/topic/sensor-readings/all";
    private static final String WS_TOPIC_READINGS_SENSOR = "/topic/sensor-readings/";
    private static final String WS_TOPIC_ALERTS = "/topic/sensor-alerts";

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final SimpMessagingTemplate messagingTemplate;
    private final SensorMapper sensorMapper;
    private final SensorReadingMapper readingMapper;
    private final SensorAlertMapper alertMapper;

    /**
     * Publish a sensor reading to Kafka and WebSocket.
     */
    @Async
    public void publishSensorReading(SensorReading reading, Sensor sensor) {
        log.debug("Publishing reading for sensor {}", sensor.getSensorCode());

        SensorReadingResponse response = readingMapper.toResponse(reading);

        // Add sensor code for context
        Map<String, Object> enrichedReading = new HashMap<>();
        enrichedReading.put("reading", response);
        enrichedReading.put("sensorCode", sensor.getSensorCode());
        enrichedReading.put("sensorType", sensor.getSensorType());

        // Publish to Kafka
        try {
            kafkaTemplate.send(TOPIC_SENSOR_TELEMETRY, sensor.getId().toString(), enrichedReading);
            log.debug("Published reading to Kafka topic: {}", TOPIC_SENSOR_TELEMETRY);
        } catch (Exception e) {
            log.error("Failed to publish reading to Kafka: {}", e.getMessage());
        }

        // Publish to WebSocket - specific sensor topic
        try {
            messagingTemplate.convertAndSend(WS_TOPIC_READINGS_SENSOR + sensor.getId(), response);
            // Also publish to the "all readings" topic
            messagingTemplate.convertAndSend(WS_TOPIC_READINGS_ALL, enrichedReading);
            log.debug("Published reading to WebSocket topics");
        } catch (Exception e) {
            log.error("Failed to publish reading to WebSocket: {}", e.getMessage());
        }
    }

    /**
     * Publish a sensor alert to Kafka and WebSocket.
     */
    @Async
    public void publishSensorAlert(SensorAlert alert, Sensor sensor) {
        log.info("Publishing alert {} for sensor {}", alert.getAlertType(), sensor.getSensorCode());

        SensorAlertResponse response = alertMapper.toResponseWithSensorCode(alert, sensor);

        // Publish to Kafka
        try {
            kafkaTemplate.send(TOPIC_SENSOR_ALERTS, sensor.getId().toString(), response);
            log.debug("Published alert to Kafka topic: {}", TOPIC_SENSOR_ALERTS);
        } catch (Exception e) {
            log.error("Failed to publish alert to Kafka: {}", e.getMessage());
        }

        // Publish to WebSocket
        try {
            messagingTemplate.convertAndSend(WS_TOPIC_ALERTS, response);
            log.debug("Published alert to WebSocket topic");
        } catch (Exception e) {
            log.error("Failed to publish alert to WebSocket: {}", e.getMessage());
        }
    }

    /**
     * Publish a sensor status change event.
     */
    @Async
    public void publishSensorStatusChanged(Sensor sensor, SensorStatus oldStatus, String reason) {
        log.info("Publishing status change for sensor {} from {} to {}",
                sensor.getSensorCode(), oldStatus, sensor.getStatus());

        Map<String, Object> event = new HashMap<>();
        event.put("sensorId", sensor.getId());
        event.put("sensorCode", sensor.getSensorCode());
        event.put("oldStatus", oldStatus);
        event.put("newStatus", sensor.getStatus());
        event.put("reason", reason);
        event.put("timestamp", Instant.now());

        // Publish to Kafka
        try {
            kafkaTemplate.send(TOPIC_SENSOR_STATUS_CHANGES, sensor.getId().toString(), event);
            log.debug("Published status change to Kafka topic: {}", TOPIC_SENSOR_STATUS_CHANGES);
        } catch (Exception e) {
            log.error("Failed to publish status change to Kafka: {}", e.getMessage());
        }
    }

    /**
     * Publish sensor created event.
     */
    @Async
    public void publishSensorCreated(Sensor sensor) {
        log.info("Publishing sensor created event for {}", sensor.getSensorCode());

        Map<String, Object> event = new HashMap<>();
        event.put("eventType", "SENSOR_CREATED");
        event.put("sensor", sensorMapper.toResponse(sensor));
        event.put("timestamp", Instant.now());

        try {
            kafkaTemplate.send(TOPIC_SENSOR_EVENTS, sensor.getId().toString(), event);
        } catch (Exception e) {
            log.error("Failed to publish sensor created event: {}", e.getMessage());
        }
    }

    /**
     * Publish sensor updated event.
     */
    @Async
    public void publishSensorUpdated(Sensor sensor) {
        log.info("Publishing sensor updated event for {}", sensor.getSensorCode());

        Map<String, Object> event = new HashMap<>();
        event.put("eventType", "SENSOR_UPDATED");
        event.put("sensor", sensorMapper.toResponse(sensor));
        event.put("timestamp", Instant.now());

        try {
            kafkaTemplate.send(TOPIC_SENSOR_EVENTS, sensor.getId().toString(), event);
        } catch (Exception e) {
            log.error("Failed to publish sensor updated event: {}", e.getMessage());
        }
    }
}
