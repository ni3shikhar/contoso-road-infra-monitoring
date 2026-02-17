package com.contoso.roadinfra.simulator.service;

import com.contoso.roadinfra.simulator.client.AssetServiceClient;
import com.contoso.roadinfra.simulator.client.AuthClient;
import com.contoso.roadinfra.simulator.client.SensorServiceClient;
import com.contoso.roadinfra.simulator.config.SimulatorConfig;
import com.contoso.roadinfra.simulator.model.*;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Main simulation orchestrator that coordinates sensor data generation,
 * failure simulation, and anomaly injection.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SimulationScheduler {

    private final SimulatorConfig config;
    private final AuthClient authClient;
    private final SensorServiceClient sensorClient;
    private final AssetServiceClient assetClient;
    private final SensorDataGenerator dataGenerator;

    private final Map<UUID, SimulatedSensor> sensors = new ConcurrentHashMap<>();
    private final AtomicBoolean initialized = new AtomicBoolean(false);
    private final AtomicLong readingCycleCount = new AtomicLong(0);
    private final AtomicLong lastFailureCheck = new AtomicLong(0);
    private final AtomicLong lastAnomalyCheck = new AtomicLong(0);
    private final Random random = new Random();

    @PostConstruct
    public void initialize() {
        if (!config.isEnabled()) {
            log.info("Simulation is disabled");
            return;
        }

        log.info("Initializing simulator...");
        
        // Schedule initialization with a delay to allow services to start
        new Thread(() -> {
            int maxRetries = 10;
            int retryDelaySeconds = 15;
            
            for (int attempt = 1; attempt <= maxRetries; attempt++) {
                try {
                    Thread.sleep(retryDelaySeconds * 1000L);
                    log.info("Attempting to initialize simulator (attempt {}/{})", attempt, maxRetries);
                    loadSensors();
                    initialized.set(true);
                    log.info("Simulator initialized with {} sensors", sensors.size());
                    return; // Success - exit retry loop
                } catch (Exception e) {
                    log.error("Failed to initialize simulator (attempt {}/{}): {}", attempt, maxRetries, e.getMessage());
                    if (attempt == maxRetries) {
                        log.error("Max retries reached. Simulator will not generate data until manually restarted.");
                    }
                }
            }
        }).start();
    }

    /**
     * Load sensors from the sensor-service.
     */
    private void loadSensors() {
        try {
            authClient.login();
            
            List<SensorResponse> sensorList = sensorClient.getAllSensors();
            
            for (SensorResponse sensor : sensorList) {
                SimulatedSensor simSensor = SimulatedSensor.builder()
                        .id(sensor.getId())
                        .sensorCode(sensor.getSensorCode())
                        .sensorType(parseSensorType(sensor.getSensorType()))
                        .assetType(sensor.getAssetType())
                        .assetId(sensor.getAssetId())
                        .status(parseSensorStatus(sensor.getStatus()))
                        .batteryLevel(sensor.getBatteryLevel())
                        .unit(sensor.getUnit())
                        .minThreshold(sensor.getMinThreshold())
                        .maxThreshold(sensor.getMaxThreshold())
                        .baselineValue(0)
                        .currentDrift(0)
                        .inAnomalyMode(false)
                        .build();
                
                sensors.put(sensor.getId(), simSensor);
            }
            
            log.info("Loaded {} sensors for simulation", sensors.size());
        } catch (Exception e) {
            log.error("Error loading sensors: {}", e.getMessage(), e);
        }
    }

    /**
     * Generate and post readings for all active sensors.
     * Runs every 10 seconds by default.
     */
    @Scheduled(fixedRateString = "${simulation.reading-interval-seconds:10}000")
    public void generateReadings() {
        if (!initialized.get() || !config.isEnabled()) {
            return;
        }

        long cycleNumber = readingCycleCount.incrementAndGet();
        log.debug("Starting reading cycle #{}", cycleNumber);

        Map<UUID, SensorReadingRequest> readings = new HashMap<>();
        int activeCount = 0;
        int skippedCount = 0;

        for (SimulatedSensor sensor : sensors.values()) {
            if (sensor.isOperational()) {
                try {
                    SensorReadingRequest reading = dataGenerator.generateReading(sensor);
                    if (reading != null) {
                        readings.put(sensor.getId(), reading);
                        activeCount++;
                        
                        // Simulate battery drain for battery-powered sensors
                        if (sensor.isBatteryPowered()) {
                            double newLevel = sensor.getBatteryLevel() - config.getBatteryDrainPerCycle();
                            sensor.setBatteryLevel(Math.max(0, newLevel));
                        }
                    }
                } catch (Exception e) {
                    log.warn("Error generating reading for sensor {}: {}", 
                            sensor.getSensorCode(), e.getMessage());
                }
            } else {
                skippedCount++;
            }
        }

        // Post readings in batch if possible
        if (!readings.isEmpty()) {
            try {
                int posted = sensorClient.postBatchReadings(readings);
                log.info("Cycle #{}: Generated {} readings, posted {}, skipped {} inactive sensors",
                        cycleNumber, activeCount, posted, skippedCount);
            } catch (Exception e) {
                log.error("Failed to post batch readings: {}", e.getMessage());
                
                // Fall back to individual posts
                int successCount = 0;
                for (Map.Entry<UUID, SensorReadingRequest> entry : readings.entrySet()) {
                    if (sensorClient.postReading(entry.getKey(), entry.getValue())) {
                        successCount++;
                    }
                }
                log.info("Fallback posting: {} of {} readings successful", successCount, readings.size());
            }
        }
    }

    /**
     * Simulate sensor failures.
     * Runs every minute, but only applies failures based on configured interval.
     */
    @Scheduled(fixedRate = 60000) // Check every minute
    public void simulateSensorFailures() {
        if (!initialized.get() || !config.isEnabled()) {
            return;
        }

        long now = System.currentTimeMillis();
        long intervalMillis = config.getFailureIntervalMinutes() * 60000L;
        
        if (now - lastFailureCheck.get() < intervalMillis) {
            return;
        }
        lastFailureCheck.set(now);

        log.info("Running sensor failure simulation");

        // Check for sensors to recover
        Instant currentTime = Instant.now();
        for (SimulatedSensor sensor : sensors.values()) {
            if (sensor.getStatus() == SensorStatus.FAULTY && 
                sensor.getFailureRecoveryTime() != null &&
                currentTime.isAfter(sensor.getFailureRecoveryTime())) {
                
                sensor.setStatus(SensorStatus.ACTIVE);
                sensor.setFailureRecoveryTime(null);
                sensorClient.updateSensorStatus(sensor.getId(), SensorStatus.ACTIVE);
                log.info("Sensor {} recovered from failure", sensor.getSensorCode());
            }
        }

        // Randomly fail 1-2 active sensors
        List<SimulatedSensor> activeSensors = sensors.values().stream()
                .filter(s -> s.getStatus() == SensorStatus.ACTIVE)
                .toList();

        if (activeSensors.isEmpty()) {
            return;
        }

        int failureCount = 1 + random.nextInt(2); // 1 or 2 failures
        List<SimulatedSensor> toFail = new ArrayList<>(activeSensors);
        Collections.shuffle(toFail);

        for (int i = 0; i < Math.min(failureCount, toFail.size()); i++) {
            SimulatedSensor sensor = toFail.get(i);
            sensor.setStatus(SensorStatus.FAULTY);
            sensor.setFailureRecoveryTime(
                    Instant.now().plusSeconds(config.getFailureRecoveryMinutes() * 60L)
            );
            sensorClient.updateSensorStatus(sensor.getId(), SensorStatus.FAULTY);
            log.info("Sensor {} set to FAULTY, will recover at {}", 
                    sensor.getSensorCode(), sensor.getFailureRecoveryTime());
        }
    }

    /**
     * Inject anomalies into sensor readings.
     * Runs every minute, but only injects based on configured interval.
     */
    @Scheduled(fixedRate = 60000) // Check every minute
    public void injectAnomalies() {
        if (!initialized.get() || !config.isEnabled()) {
            return;
        }

        long now = System.currentTimeMillis();
        long intervalMillis = config.getAnomalyIntervalMinutes() * 60000L;
        
        if (now - lastAnomalyCheck.get() < intervalMillis) {
            return;
        }
        lastAnomalyCheck.set(now);

        log.info("Running anomaly injection");

        // Clear old anomalies (after 5 minutes)
        Instant cutoff = Instant.now().minusSeconds(300);
        for (SimulatedSensor sensor : sensors.values()) {
            if (sensor.isInAnomalyMode() && 
                sensor.getAnomalyStartTime() != null &&
                sensor.getAnomalyStartTime().isBefore(cutoff)) {
                
                sensor.setInAnomalyMode(false);
                sensor.setCurrentAnomaly(null);
                sensor.setAnomalyStartTime(null);
                sensor.setCurrentDrift(0);
                log.info("Cleared anomaly from sensor {}", sensor.getSensorCode());
            }
        }

        // Inject new anomaly into one random active sensor
        List<SimulatedSensor> candidates = sensors.values().stream()
                .filter(s -> s.getStatus() == SensorStatus.ACTIVE && !s.isInAnomalyMode())
                .toList();

        if (!candidates.isEmpty()) {
            SimulatedSensor target = candidates.get(random.nextInt(candidates.size()));
            AnomalyType anomaly = AnomalyType.values()[random.nextInt(AnomalyType.values().length)];
            
            target.setInAnomalyMode(true);
            target.setCurrentAnomaly(anomaly);
            target.setAnomalyStartTime(Instant.now());
            target.setCurrentDrift(0);
            
            log.info("Injected {} anomaly into sensor {}", anomaly, target.getSensorCode());
        }
    }

    /**
     * Refresh sensor list periodically.
     */
    @Scheduled(fixedRate = 300000) // Every 5 minutes
    public void refreshSensors() {
        if (!initialized.get() || !config.isEnabled()) {
            return;
        }

        try {
            List<SensorResponse> sensorList = sensorClient.getAllSensors();
            
            // Add new sensors
            for (SensorResponse sensor : sensorList) {
                if (!sensors.containsKey(sensor.getId())) {
                    SimulatedSensor simSensor = SimulatedSensor.builder()
                            .id(sensor.getId())
                            .sensorCode(sensor.getSensorCode())
                            .sensorType(parseSensorType(sensor.getSensorType()))
                            .assetType(sensor.getAssetType())
                            .assetId(sensor.getAssetId())
                            .status(parseSensorStatus(sensor.getStatus()))
                            .batteryLevel(sensor.getBatteryLevel())
                            .unit(sensor.getUnit())
                            .minThreshold(sensor.getMinThreshold())
                            .maxThreshold(sensor.getMaxThreshold())
                            .baselineValue(0)
                            .currentDrift(0)
                            .inAnomalyMode(false)
                            .build();
                    
                    sensors.put(sensor.getId(), simSensor);
                    log.info("Added new sensor to simulation: {}", sensor.getSensorCode());
                }
            }
            
            log.debug("Sensor refresh complete, {} sensors in simulation", sensors.size());
        } catch (Exception e) {
            log.warn("Error refreshing sensors: {}", e.getMessage());
        }
    }

    private SensorType parseSensorType(String type) {
        try {
            return SensorType.fromDisplayName(type);
        } catch (Exception e) {
            return SensorType.TEMPERATURE; // Default fallback
        }
    }

    private SensorStatus parseSensorStatus(String status) {
        try {
            return SensorStatus.fromDisplayName(status);
        } catch (Exception e) {
            return SensorStatus.ACTIVE; // Default fallback
        }
    }

    /**
     * Get simulation statistics.
     */
    public Map<String, Object> getStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("initialized", initialized.get());
        stats.put("enabled", config.isEnabled());
        stats.put("totalSensors", sensors.size());
        stats.put("activeSensors", sensors.values().stream()
                .filter(s -> s.getStatus() == SensorStatus.ACTIVE).count());
        stats.put("faultySensors", sensors.values().stream()
                .filter(s -> s.getStatus() == SensorStatus.FAULTY).count());
        stats.put("sensorsWithAnomaly", sensors.values().stream()
                .filter(SimulatedSensor::isInAnomalyMode).count());
        stats.put("readingCycles", readingCycleCount.get());
        return stats;
    }
}
