package com.contoso.roadinfra.sensor.service;

import com.contoso.roadinfra.common.constants.AlertSeverity;
import com.contoso.roadinfra.common.constants.DataQuality;
import com.contoso.roadinfra.common.constants.SensorAlertType;
import com.contoso.roadinfra.common.exception.ResourceNotFoundException;
import com.contoso.roadinfra.sensor.dto.*;
import com.contoso.roadinfra.sensor.entity.Sensor;
import com.contoso.roadinfra.sensor.entity.SensorAlert;
import com.contoso.roadinfra.sensor.entity.SensorReading;
import com.contoso.roadinfra.sensor.event.SensorEventPublisher;
import com.contoso.roadinfra.sensor.mapper.SensorReadingMapper;
import com.contoso.roadinfra.sensor.repository.SensorAlertRepository;
import com.contoso.roadinfra.sensor.repository.SensorReadingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Service for managing sensor readings and telemetry data.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class SensorReadingService {

    private final SensorReadingRepository readingRepository;
    private final SensorAlertRepository alertRepository;
    private final SensorReadingMapper readingMapper;
    private final SensorService sensorService;
    private final SensorEventPublisher eventPublisher;

    /**
     * Ingest a new sensor reading.
     */
    public SensorReadingResponse ingestReading(UUID sensorId, SensorReadingRequest request) {
        log.debug("Ingesting reading for sensor {}: {}", sensorId, request.getValue());

        Sensor sensor = sensorService.getSensorEntityById(sensorId);

        // Create the reading entity
        SensorReading reading = readingMapper.toEntityWithDefaults(request, sensorId);

        // Check for threshold breach
        boolean isAnomaly = checkThresholdBreach(sensor, reading.getValue());
        reading.setAnomaly(isAnomaly);

        if (isAnomaly) {
            reading.setAnomalyScore(calculateAnomalyScore(sensor, reading.getValue()));
            // Create alert for threshold breach
            createThresholdBreachAlert(sensor, reading);
        }

        // Save the reading
        SensorReading saved = readingRepository.save(reading);

        // Update sensor's current value and last data received
        sensorService.updateSensorDataReceived(sensorId, reading.getValue(), reading.getTimestamp());

        // Publish to WebSocket and Kafka
        eventPublisher.publishSensorReading(saved, sensor);

        return readingMapper.toResponse(saved);
    }

    /**
     * Batch ingest multiple sensor readings.
     */
    public List<SensorReadingResponse> batchIngestReadings(BatchReadingRequest request) {
        log.info("Batch ingesting {} readings", request.getReadings().size());

        List<SensorReadingResponse> results = new ArrayList<>();

        for (BatchReadingRequest.BatchReadingItem item : request.getReadings()) {
            try {
                // Convert flat BatchReadingItem to SensorReadingRequest
                SensorReadingRequest readingRequest = SensorReadingRequest.builder()
                        .timestamp(item.getTimestamp())
                        .value(item.getValue())
                        .unit(item.getUnit())
                        .secondaryValue(item.getSecondaryValue())
                        .tertiaryValue(item.getTertiaryValue())
                        .quality(item.getQuality())
                        .rawPayload(item.getRawPayload())
                        .build();
                
                SensorReadingResponse response = ingestReading(item.getSensorId(), readingRequest);
                results.add(response);
            } catch (Exception e) {
                log.error("Failed to ingest reading for sensor {}: {}", item.getSensorId(), e.getMessage());
                // Continue with other readings
            }
        }

        log.info("Batch ingest completed: {} of {} readings successful",
                results.size(), request.getReadings().size());

        return results;
    }

    /**
     * Get readings for a sensor with pagination.
     */
    @Transactional(readOnly = true)
    public Page<SensorReadingResponse> getReadings(UUID sensorId, Pageable pageable) {
        log.debug("Fetching readings for sensor {}", sensorId);

        // Verify sensor exists
        sensorService.getSensorEntityById(sensorId);

        return readingRepository.findBySensorIdOrderByTimestampDesc(sensorId, pageable)
                .map(readingMapper::toResponse);
    }

    /**
     * Get readings for a sensor within a time range.
     */
    @Transactional(readOnly = true)
    public List<SensorReadingResponse> getReadingsInRange(UUID sensorId, Instant start, Instant end) {
        log.debug("Fetching readings for sensor {} between {} and {}", sensorId, start, end);

        // Verify sensor exists
        sensorService.getSensorEntityById(sensorId);

        return readingMapper.toResponseList(
                readingRepository.findBySensorIdAndTimestampBetweenOrderByTimestampDesc(sensorId, start, end));
    }

    /**
     * Get the latest reading for a sensor.
     */
    @Transactional(readOnly = true)
    public SensorReadingResponse getLatestReading(UUID sensorId) {
        log.debug("Fetching latest reading for sensor {}", sensorId);

        // Verify sensor exists
        sensorService.getSensorEntityById(sensorId);

        return readingRepository.findLatestBySensorId(sensorId)
                .map(readingMapper::toResponse)
                .orElse(null);
    }

    /**
     * Get aggregated statistics for sensor readings.
     */
    @Transactional(readOnly = true)
    public SensorReadingStatsResponse getReadingStats(UUID sensorId, Instant start, Instant end) {
        log.debug("Calculating stats for sensor {} between {} and {}", sensorId, start, end);

        Sensor sensor = sensorService.getSensorEntityById(sensorId);

        // If no range specified, default to last 24 hours
        if (start == null) {
            start = Instant.now().minus(24, ChronoUnit.HOURS);
        }
        if (end == null) {
            end = Instant.now();
        }

        Long readingCount = readingRepository.countReadingsInPeriod(sensorId, start, end);
        Double minValue = readingRepository.findMinValue(sensorId, start, end);
        Double maxValue = readingRepository.findMaxValue(sensorId, start, end);
        Double avgValue = readingRepository.calculateAverageValue(sensorId, start, end);
        Double stdDev = readingRepository.calculateStdDeviation(sensorId, start, end);
        Long anomalyCount = readingRepository.countAnomaliesInPeriod(sensorId, start, end);

        return SensorReadingStatsResponse.builder()
                .sensorId(sensorId)
                .periodStart(start)
                .periodEnd(end)
                .readingCount(readingCount != null ? readingCount : 0L)
                .minValue(minValue)
                .maxValue(maxValue)
                .avgValue(avgValue)
                .stdDeviation(stdDev)
                .anomalyCount(anomalyCount != null ? anomalyCount : 0L)
                .unit(sensor.getUnit())
                .build();
    }

    /**
     * Check if a reading value breaches configured thresholds.
     */
    private boolean checkThresholdBreach(Sensor sensor, Double value) {
        if (value == null) {
            return false;
        }

        if (sensor.getMinThreshold() != null && value < sensor.getMinThreshold()) {
            return true;
        }
        if (sensor.getMaxThreshold() != null && value > sensor.getMaxThreshold()) {
            return true;
        }
        return false;
    }

    /**
     * Calculate anomaly score based on how far the value is from threshold.
     */
    private Double calculateAnomalyScore(Sensor sensor, Double value) {
        if (value == null) {
            return 0.0;
        }

        Double minThreshold = sensor.getMinThreshold();
        Double maxThreshold = sensor.getMaxThreshold();

        if (minThreshold != null && maxThreshold != null) {
            double range = maxThreshold - minThreshold;
            if (range > 0) {
                if (value < minThreshold) {
                    return Math.min(100.0, ((minThreshold - value) / range) * 100);
                } else if (value > maxThreshold) {
                    return Math.min(100.0, ((value - maxThreshold) / range) * 100);
                }
            }
        } else if (minThreshold != null && value < minThreshold) {
            return Math.min(100.0, ((minThreshold - value) / Math.abs(minThreshold)) * 100);
        } else if (maxThreshold != null && value > maxThreshold) {
            return Math.min(100.0, ((value - maxThreshold) / Math.abs(maxThreshold)) * 100);
        }

        return 0.0;
    }

    /**
     * Create an alert for threshold breach.
     */
    private void createThresholdBreachAlert(Sensor sensor, SensorReading reading) {
        // Check if there's already a recent unacknowledged alert
        Instant recentThreshold = Instant.now().minus(15, ChronoUnit.MINUTES);
        if (alertRepository.existsRecentUnacknowledgedAlert(sensor.getId(),
                SensorAlertType.THRESHOLD_BREACH, recentThreshold)) {
            log.debug("Recent unacknowledged threshold alert exists for sensor {}, skipping", sensor.getId());
            return;
        }

        AlertSeverity severity = determineSeverity(sensor, reading.getValue());

        String message = String.format("Sensor %s reading %.2f %s breached %s threshold of %.2f %s",
                sensor.getSensorCode(),
                reading.getValue(),
                sensor.getUnit() != null ? sensor.getUnit() : "",
                reading.getValue() < sensor.getMinThreshold() ? "minimum" : "maximum",
                reading.getValue() < sensor.getMinThreshold() ? sensor.getMinThreshold() : sensor.getMaxThreshold(),
                sensor.getUnit() != null ? sensor.getUnit() : "");

        SensorAlert alert = SensorAlert.builder()
                .sensorId(sensor.getId())
                .alertType(SensorAlertType.THRESHOLD_BREACH)
                .message(message)
                .severity(severity)
                .readingValue(reading.getValue())
                .thresholdValue(reading.getValue() < sensor.getMinThreshold() ?
                        sensor.getMinThreshold() : sensor.getMaxThreshold())
                .acknowledged(false)
                .build();

        SensorAlert savedAlert = alertRepository.save(alert);
        log.info("Created threshold breach alert {} for sensor {}", savedAlert.getId(), sensor.getSensorCode());

        // Publish alert
        eventPublisher.publishSensorAlert(savedAlert, sensor);
    }

    /**
     * Determine alert severity based on how far the value is from threshold.
     */
    private AlertSeverity determineSeverity(Sensor sensor, Double value) {
        Double minThreshold = sensor.getMinThreshold();
        Double maxThreshold = sensor.getMaxThreshold();

        if (minThreshold != null && maxThreshold != null) {
            double range = maxThreshold - minThreshold;
            double deviation;

            if (value < minThreshold) {
                deviation = (minThreshold - value) / range;
            } else {
                deviation = (value - maxThreshold) / range;
            }

            if (deviation > 0.5) {
                return AlertSeverity.CRITICAL;
            } else if (deviation > 0.25) {
                return AlertSeverity.HIGH;
            } else if (deviation > 0.1) {
                return AlertSeverity.MEDIUM;
            }
        }

        return AlertSeverity.LOW;
    }
}
