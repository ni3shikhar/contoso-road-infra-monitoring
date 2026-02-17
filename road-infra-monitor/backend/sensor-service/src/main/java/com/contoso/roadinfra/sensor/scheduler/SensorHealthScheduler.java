package com.contoso.roadinfra.sensor.scheduler;

import com.contoso.roadinfra.common.constants.AlertSeverity;
import com.contoso.roadinfra.common.constants.SensorAlertType;
import com.contoso.roadinfra.common.constants.SensorStatus;
import com.contoso.roadinfra.sensor.entity.Sensor;
import com.contoso.roadinfra.sensor.service.SensorAlertService;
import com.contoso.roadinfra.sensor.service.SensorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Scheduled tasks for monitoring sensor health.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SensorHealthScheduler {

    private final SensorService sensorService;
    private final SensorAlertService alertService;

    @Value("${sensor.health.offline-threshold-minutes:15}")
    private int offlineThresholdMinutes;

    @Value("${sensor.health.low-battery-threshold:20.0}")
    private double lowBatteryThreshold;

    /**
     * Check for sensors that haven't reported data recently (every 5 minutes).
     * Marks them as OFFLINE and generates alerts.
     */
    @Scheduled(fixedRateString = "${sensor.health.offline-check-interval-ms:300000}") // 5 minutes
    @Transactional
    public void checkForOfflineSensors() {
        log.info("Running offline sensor check (threshold: {} minutes)", offlineThresholdMinutes);

        List<Sensor> offlineSensors = sensorService.findOfflineSensors(offlineThresholdMinutes);

        if (offlineSensors.isEmpty()) {
            log.debug("No offline sensors detected");
            return;
        }

        log.warn("Detected {} sensors with no recent data", offlineSensors.size());

        // Get sensor IDs to update
        List<java.util.UUID> sensorIds = offlineSensors.stream()
                .filter(s -> s.getStatus() != SensorStatus.OFFLINE)
                .map(Sensor::getId)
                .collect(Collectors.toList());

        if (!sensorIds.isEmpty()) {
            // Bulk update status
            int updated = sensorService.bulkUpdateStatus(sensorIds, SensorStatus.OFFLINE, "SYSTEM");
            log.info("Marked {} sensors as OFFLINE", updated);
        }

        // Generate alerts for each sensor
        for (Sensor sensor : offlineSensors) {
            try {
                String message = String.format("Sensor %s has not reported data for more than %d minutes",
                        sensor.getSensorCode(), offlineThresholdMinutes);

                alertService.createAlert(
                        sensor.getId(),
                        SensorAlertType.OFFLINE,
                        message,
                        AlertSeverity.HIGH,
                        null,
                        null
                );
            } catch (Exception e) {
                log.error("Failed to create offline alert for sensor {}: {}",
                        sensor.getSensorCode(), e.getMessage());
            }
        }
    }

    /**
     * Check for sensors due for calibration (daily at 2 AM).
     */
    @Scheduled(cron = "${sensor.health.calibration-check-cron:0 0 2 * * *}")
    @Transactional
    public void checkForCalibrationDue() {
        log.info("Running calibration due check");

        List<Sensor> sensorsDue = sensorService.findSensorsDueForCalibration();

        if (sensorsDue.isEmpty()) {
            log.debug("No sensors due for calibration");
            return;
        }

        log.info("Found {} sensors due for calibration", sensorsDue.size());

        for (Sensor sensor : sensorsDue) {
            try {
                String message = String.format(
                        "Sensor %s is due for calibration. Last calibrated: %s, Interval: %d days",
                        sensor.getSensorCode(),
                        sensor.getLastCalibrationDate(),
                        sensor.getCalibrationIntervalDays()
                );

                alertService.createAlert(
                        sensor.getId(),
                        SensorAlertType.CALIBRATION_DUE,
                        message,
                        AlertSeverity.MEDIUM,
                        null,
                        null
                );
            } catch (Exception e) {
                log.error("Failed to create calibration due alert for sensor {}: {}",
                        sensor.getSensorCode(), e.getMessage());
            }
        }
    }

    /**
     * Check for sensors with low battery (every hour).
     */
    @Scheduled(fixedRateString = "${sensor.health.battery-check-interval-ms:3600000}") // 1 hour
    @Transactional(readOnly = true)
    public void checkForLowBattery() {
        log.info("Running low battery check (threshold: {}%)", lowBatteryThreshold);

        List<Sensor> lowBatterySensors = sensorService.findOfflineSensors(0).stream()
                .filter(s -> s.getBatteryLevel() != null && s.getBatteryLevel() < lowBatteryThreshold)
                .filter(s -> s.getStatus() == SensorStatus.ACTIVE)
                .toList();

        if (lowBatterySensors.isEmpty()) {
            log.debug("No sensors with low battery detected");
            return;
        }

        log.warn("Detected {} sensors with low battery", lowBatterySensors.size());

        for (Sensor sensor : lowBatterySensors) {
            try {
                AlertSeverity severity = sensor.getBatteryLevel() < 10 ?
                        AlertSeverity.HIGH : AlertSeverity.MEDIUM;

                String message = String.format("Sensor %s battery level is low: %.1f%%",
                        sensor.getSensorCode(), sensor.getBatteryLevel());

                alertService.createAlert(
                        sensor.getId(),
                        SensorAlertType.LOW_BATTERY,
                        message,
                        severity,
                        sensor.getBatteryLevel(),
                        lowBatteryThreshold
                );
            } catch (Exception e) {
                log.error("Failed to create low battery alert for sensor {}: {}",
                        sensor.getSensorCode(), e.getMessage());
            }
        }
    }
}
