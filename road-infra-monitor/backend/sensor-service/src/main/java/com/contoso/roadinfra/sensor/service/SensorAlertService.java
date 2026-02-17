package com.contoso.roadinfra.sensor.service;

import com.contoso.roadinfra.common.constants.AlertSeverity;
import com.contoso.roadinfra.common.constants.SensorAlertType;
import com.contoso.roadinfra.common.exception.ResourceNotFoundException;
import com.contoso.roadinfra.sensor.dto.SensorAlertResponse;
import com.contoso.roadinfra.sensor.entity.Sensor;
import com.contoso.roadinfra.sensor.entity.SensorAlert;
import com.contoso.roadinfra.sensor.event.SensorEventPublisher;
import com.contoso.roadinfra.sensor.mapper.SensorAlertMapper;
import com.contoso.roadinfra.sensor.repository.SensorAlertRepository;
import com.contoso.roadinfra.sensor.repository.SensorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Service for managing sensor alerts.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class SensorAlertService {

    private final SensorAlertRepository alertRepository;
    private final SensorRepository sensorRepository;
    private final SensorAlertMapper alertMapper;
    private final SensorEventPublisher eventPublisher;

    /**
     * Get all alerts with pagination.
     */
    @Transactional(readOnly = true)
    public Page<SensorAlertResponse> getAllAlerts(Pageable pageable) {
        log.debug("Fetching all alerts with pagination");

        Page<SensorAlert> alerts = alertRepository.findAllByOrderByCreatedAtDesc(pageable);

        // Load sensors for sensor codes
        Map<UUID, Sensor> sensorMap = loadSensorsForAlerts(alerts);

        return alerts.map(alert -> alertMapper.toResponseWithSensorCode(alert, sensorMap.get(alert.getSensorId())));
    }

    /**
     * Get unacknowledged alerts with pagination.
     */
    @Transactional(readOnly = true)
    public Page<SensorAlertResponse> getUnacknowledgedAlerts(Pageable pageable) {
        log.debug("Fetching unacknowledged alerts");

        Page<SensorAlert> alerts = alertRepository.findByAcknowledgedFalseOrderByCreatedAtDesc(pageable);

        Map<UUID, Sensor> sensorMap = loadSensorsForAlerts(alerts);

        return alerts.map(alert -> alertMapper.toResponseWithSensorCode(alert, sensorMap.get(alert.getSensorId())));
    }

    /**
     * Get alerts by severity.
     */
    @Transactional(readOnly = true)
    public Page<SensorAlertResponse> getAlertsBySeverity(AlertSeverity severity, Pageable pageable) {
        log.debug("Fetching alerts with severity: {}", severity);

        Page<SensorAlert> alerts = alertRepository.findBySeverityOrderByCreatedAtDesc(severity, pageable);

        Map<UUID, Sensor> sensorMap = loadSensorsForAlerts(alerts);

        return alerts.map(alert -> alertMapper.toResponseWithSensorCode(alert, sensorMap.get(alert.getSensorId())));
    }

    /**
     * Get alerts by type.
     */
    @Transactional(readOnly = true)
    public Page<SensorAlertResponse> getAlertsByType(SensorAlertType alertType, Pageable pageable) {
        log.debug("Fetching alerts of type: {}", alertType);

        Page<SensorAlert> alerts = alertRepository.findByAlertTypeOrderByCreatedAtDesc(alertType, pageable);

        Map<UUID, Sensor> sensorMap = loadSensorsForAlerts(alerts);

        return alerts.map(alert -> alertMapper.toResponseWithSensorCode(alert, sensorMap.get(alert.getSensorId())));
    }

    /**
     * Acknowledge an alert.
     */
    public SensorAlertResponse acknowledgeAlert(UUID alertId, String username) {
        log.info("Acknowledging alert {} by user {}", alertId, username);

        SensorAlert alert = alertRepository.findById(alertId)
                .orElseThrow(() -> new ResourceNotFoundException("SensorAlert", alertId));

        if (alert.getAcknowledged()) {
            log.warn("Alert {} is already acknowledged", alertId);
            Sensor sensor = sensorRepository.findById(alert.getSensorId()).orElse(null);
            return alertMapper.toResponseWithSensorCode(alert, sensor);
        }

        alert.acknowledge(username);
        SensorAlert saved = alertRepository.save(alert);

        log.info("Alert {} acknowledged by {}", alertId, username);

        Sensor sensor = sensorRepository.findById(alert.getSensorId()).orElse(null);
        return alertMapper.toResponseWithSensorCode(saved, sensor);
    }

    /**
     * Create an alert programmatically.
     */
    public SensorAlert createAlert(UUID sensorId, SensorAlertType alertType, String message,
                                   AlertSeverity severity, Double readingValue, Double thresholdValue) {
        log.info("Creating {} alert for sensor {}", alertType, sensorId);

        Sensor sensor = sensorRepository.findById(sensorId)
                .orElseThrow(() -> new ResourceNotFoundException("Sensor", sensorId));

        // Check for recent duplicate
        Instant recentThreshold = Instant.now().minusSeconds(15 * 60); // 15 minutes
        if (alertRepository.existsRecentUnacknowledgedAlert(sensorId, alertType, recentThreshold)) {
            log.debug("Recent unacknowledged {} alert exists for sensor {}, skipping", alertType, sensorId);
            return null;
        }

        SensorAlert alert = SensorAlert.builder()
                .sensorId(sensorId)
                .alertType(alertType)
                .message(message)
                .severity(severity)
                .readingValue(readingValue)
                .thresholdValue(thresholdValue)
                .acknowledged(false)
                .build();

        SensorAlert saved = alertRepository.save(alert);
        log.info("Created {} alert {} for sensor {}", alertType, saved.getId(), sensor.getSensorCode());

        // Publish alert
        eventPublisher.publishSensorAlert(saved, sensor);

        return saved;
    }

    /**
     * Get count of unacknowledged alerts.
     */
    @Transactional(readOnly = true)
    public Long getUnacknowledgedAlertCount() {
        return alertRepository.countAllUnacknowledged();
    }

    /**
     * Load sensors for a page of alerts (to include sensor code in response).
     */
    private Map<UUID, Sensor> loadSensorsForAlerts(Page<SensorAlert> alerts) {
        return alerts.getContent().stream()
                .map(SensorAlert::getSensorId)
                .distinct()
                .map(sensorRepository::findById)
                .filter(java.util.Optional::isPresent)
                .map(java.util.Optional::get)
                .collect(Collectors.toMap(Sensor::getId, Function.identity()));
    }
}
