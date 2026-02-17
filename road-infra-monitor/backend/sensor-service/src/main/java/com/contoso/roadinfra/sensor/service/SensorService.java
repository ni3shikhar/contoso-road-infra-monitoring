package com.contoso.roadinfra.sensor.service;

import com.contoso.roadinfra.common.constants.SensorStatus;
import com.contoso.roadinfra.common.constants.SensorType;
import com.contoso.roadinfra.common.exception.ResourceNotFoundException;
import com.contoso.roadinfra.sensor.dto.*;
import com.contoso.roadinfra.sensor.entity.Sensor;
import com.contoso.roadinfra.sensor.event.SensorEventPublisher;
import com.contoso.roadinfra.sensor.mapper.SensorMapper;
import com.contoso.roadinfra.sensor.repository.SensorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for managing IoT sensors deployed on the road infrastructure.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class SensorService {

    private final SensorRepository sensorRepository;
    private final SensorMapper sensorMapper;
    private final SensorEventPublisher eventPublisher;

    /**
     * Get a sensor by its ID.
     */
    @Cacheable(value = "sensors", key = "#id")
    @Transactional(readOnly = true)
    public SensorResponse getSensorById(UUID id) {
        log.debug("Fetching sensor with id: {}", id);
        Sensor sensor = sensorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Sensor", id));
        return sensorMapper.toResponse(sensor);
    }

    /**
     * Get a sensor by its unique code.
     */
    @Transactional(readOnly = true)
    public SensorResponse getSensorByCode(String sensorCode) {
        log.debug("Fetching sensor with code: {}", sensorCode);
        Sensor sensor = sensorRepository.findBySensorCode(sensorCode)
                .orElseThrow(() -> new ResourceNotFoundException("Sensor", sensorCode));
        return sensorMapper.toResponse(sensor);
    }

    /**
     * Get all sensors with pagination.
     */
    @Transactional(readOnly = true)
    public Page<SensorResponse> getAllSensors(Pageable pageable) {
        log.debug("Fetching all sensors with pagination");
        return sensorRepository.findAll(pageable)
                .map(sensorMapper::toResponse);
    }

    /**
     * Get all sensors for a specific asset.
     */
    @Transactional(readOnly = true)
    public List<SensorResponse> getSensorsByAssetId(UUID assetId) {
        log.debug("Fetching sensors for asset: {}", assetId);
        return sensorMapper.toResponseList(sensorRepository.findByAssetId(assetId));
    }

    /**
     * Get sensors grouped by type with counts.
     */
    @Transactional(readOnly = true)
    public List<SensorCountByTypeResponse> getSensorCountsByType() {
        log.debug("Fetching sensor counts by type");
        return sensorRepository.countGroupedByType().stream()
                .map(row -> SensorCountByTypeResponse.builder()
                        .sensorType((SensorType) row[0])
                        .count((Long) row[1])
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * Get sensors grouped by status with counts.
     */
    @Transactional(readOnly = true)
    public List<SensorCountByStatusResponse> getSensorCountsByStatus() {
        log.debug("Fetching sensor counts by status");
        return sensorRepository.countGroupedByStatus().stream()
                .map(row -> SensorCountByStatusResponse.builder()
                        .status((SensorStatus) row[0])
                        .count((Long) row[1])
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * Register a new sensor.
     */
    @CacheEvict(value = "sensors", allEntries = true)
    public SensorResponse createSensor(SensorCreateRequest request, String username) {
        log.info("Creating new sensor: {} by user {}", request.getSensorCode(), username);

        // Check for duplicate sensor code
        if (sensorRepository.existsBySensorCode(request.getSensorCode())) {
            throw new IllegalArgumentException("Sensor with code " + request.getSensorCode() + " already exists");
        }

        Sensor sensor = sensorMapper.toEntity(request);
        sensor.setStatus(SensorStatus.INACTIVE);
        sensor.setCreatedBy(username);
        sensor.setUpdatedBy(username);

        Sensor saved = sensorRepository.save(sensor);
        log.info("Sensor created with ID: {}", saved.getId());

        // Publish event
        eventPublisher.publishSensorCreated(saved);

        return sensorMapper.toResponse(saved);
    }

    /**
     * Update an existing sensor.
     */
    @CacheEvict(value = "sensors", key = "#id")
    public SensorResponse updateSensor(UUID id, SensorUpdateRequest request, String username) {
        log.info("Updating sensor with id: {} by user {}", id, username);
        Sensor sensor = sensorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Sensor", id));

        sensorMapper.updateEntity(request, sensor);
        sensor.setUpdatedBy(username);

        Sensor updated = sensorRepository.save(sensor);
        log.info("Sensor {} updated successfully", id);

        // Publish event
        eventPublisher.publishSensorUpdated(updated);

        return sensorMapper.toResponse(updated);
    }

    /**
     * Update sensor status.
     */
    @CacheEvict(value = "sensors", key = "#id")
    public SensorResponse updateSensorStatus(UUID id, SensorStatusUpdateRequest request, String username) {
        log.info("Updating status of sensor {} to {} by user {}", id, request.getStatus(), username);
        Sensor sensor = sensorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Sensor", id));

        SensorStatus oldStatus = sensor.getStatus();
        sensor.setStatus(request.getStatus());
        sensor.setUpdatedBy(username);

        Sensor updated = sensorRepository.save(sensor);
        log.info("Sensor {} status changed from {} to {}", id, oldStatus, request.getStatus());

        // Publish status change event
        eventPublisher.publishSensorStatusChanged(updated, oldStatus, request.getReason());

        return sensorMapper.toResponse(updated);
    }

    /**
     * Decommission a sensor (soft delete).
     */
    @CacheEvict(value = "sensors", key = "#id")
    public void decommissionSensor(UUID id, String username) {
        log.info("Decommissioning sensor with id: {} by user {}", id, username);
        Sensor sensor = sensorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Sensor", id));

        SensorStatus oldStatus = sensor.getStatus();
        sensor.setStatus(SensorStatus.DECOMMISSIONED);
        sensor.setUpdatedBy(username);

        sensorRepository.save(sensor);
        log.info("Sensor {} decommissioned", id);

        // Publish status change event
        eventPublisher.publishSensorStatusChanged(sensor, oldStatus, "Decommissioned by " + username);
    }

    /**
     * Get the sensor entity by ID (internal use).
     */
    @Transactional(readOnly = true)
    public Sensor getSensorEntityById(UUID id) {
        return sensorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Sensor", id));
    }

    /**
     * Update sensor's last data received timestamp and current value.
     */
    public void updateSensorDataReceived(UUID sensorId, Double value, Instant timestamp) {
        sensorRepository.findById(sensorId).ifPresent(sensor -> {
            sensor.setCurrentValue(value);
            sensor.setLastDataReceivedAt(timestamp);
            if (sensor.getStatus() == SensorStatus.OFFLINE) {
                sensor.setStatus(SensorStatus.ACTIVE);
            }
            sensorRepository.save(sensor);
        });
    }

    /**
     * Find sensors that haven't reported data recently.
     */
    @Transactional(readOnly = true)
    public List<Sensor> findOfflineSensors(int thresholdMinutes) {
        Instant threshold = Instant.now().minusSeconds(thresholdMinutes * 60L);
        return sensorRepository.findSensorsWithNoRecentData(threshold);
    }

    /**
     * Find sensors due for calibration.
     */
    @Transactional(readOnly = true)
    public List<Sensor> findSensorsDueForCalibration() {
        return sensorRepository.findSensorsDueForCalibrationNative(java.time.LocalDate.now());
    }

    /**
     * Update status of multiple sensors.
     */
    public int bulkUpdateStatus(List<UUID> sensorIds, SensorStatus newStatus, String username) {
        return sensorRepository.updateStatusForSensors(sensorIds, newStatus, Instant.now(), username);
    }
}
