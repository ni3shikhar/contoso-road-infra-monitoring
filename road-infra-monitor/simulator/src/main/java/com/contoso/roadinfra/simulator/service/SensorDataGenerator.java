package com.contoso.roadinfra.simulator.service;

import com.contoso.roadinfra.simulator.model.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Random;

/**
 * Generates realistic sensor readings based on sensor type and simulation parameters.
 * Each sensor type has specific normal ranges, units, and behavior patterns.
 */
@Slf4j
@Service
public class SensorDataGenerator {

    private final Random random = new Random();

    /**
     * Generate a reading for the given sensor based on its type and current state.
     */
    public SensorReadingRequest generateReading(SimulatedSensor sensor) {
        if (!sensor.isOperational()) {
            return null;
        }

        SensorType type;
        try {
            type = SensorType.fromDisplayName(sensor.getSensorType().toString());
        } catch (Exception e) {
            log.warn("Unknown sensor type: {}, using default generator", sensor.getSensorType());
            return generateDefaultReading(sensor);
        }

        SensorReadingRequest reading = switch (type) {
            case STRAIN_GAUGE -> generateStrainGaugeReading(sensor);
            case ACCELEROMETER -> generateAccelerometerReading(sensor);
            case TEMPERATURE -> generateTemperatureReading(sensor);
            case DISPLACEMENT -> generateDisplacementReading(sensor);
            case CRACK_METER -> generateCrackMeterReading(sensor);
            case TILTMETER -> generateTiltmeterReading(sensor);
            case GPS -> generateGpsReading(sensor);
            case MOISTURE -> generateMoistureReading(sensor);
            case AIR_QUALITY -> generateAirQualityReading(sensor);
            case FLOW_SENSOR -> generateFlowSensorReading(sensor);
            case LEVEL_SENSOR -> generateLevelSensorReading(sensor);
            case IMPACT_SENSOR -> generateImpactSensorReading(sensor);
            default -> generateDefaultReading(sensor);
        };

        // Apply anomaly if active
        if (sensor.isInAnomalyMode() && reading != null) {
            reading = applyAnomaly(sensor, reading);
        }

        return reading;
    }

    /**
     * Strain gauges: normal range 50-200 microstrain, occasional spikes to 350+
     */
    private SensorReadingRequest generateStrainGaugeReading(SimulatedSensor sensor) {
        double baseValue = 125.0; // Center of normal range
        double variation = 75.0;  // ±75 for 50-200 range
        
        // Add some traffic-induced variation (higher during day)
        double hourFactor = getHourlyTrafficFactor();
        double value = baseValue + (random.nextGaussian() * variation * 0.3);
        value += hourFactor * 30; // Traffic adds up to 30 microstrain
        
        // Occasional spikes (1% chance)
        if (random.nextDouble() < 0.01) {
            value = 350 + random.nextDouble() * 100; // Spike to 350-450
        }
        
        value = Math.max(0, value); // No negative strain
        
        return SensorReadingRequest.builder()
                .timestamp(Instant.now())
                .value(Math.round(value * 10) / 10.0)
                .unit("μstrain")
                .quality("GOOD")
                .build();
    }

    /**
     * Accelerometers: normal 0.01-0.05 g, vibration events up to 0.3g
     */
    private SensorReadingRequest generateAccelerometerReading(SimulatedSensor sensor) {
        double baseValue = 0.03; // Center of normal range
        double x = baseValue + random.nextGaussian() * 0.01;
        double y = baseValue + random.nextGaussian() * 0.01;
        double z = baseValue + random.nextGaussian() * 0.01;
        
        // Traffic-induced vibration
        double hourFactor = getHourlyTrafficFactor();
        x += hourFactor * 0.02;
        y += hourFactor * 0.02;
        z += hourFactor * 0.015;
        
        // Occasional vibration event (0.5% chance)
        if (random.nextDouble() < 0.005) {
            double spike = 0.15 + random.nextDouble() * 0.15; // 0.15-0.3g
            int axis = random.nextInt(3);
            if (axis == 0) x = spike;
            else if (axis == 1) y = spike;
            else z = spike;
        }
        
        return SensorReadingRequest.builder()
                .timestamp(Instant.now())
                .value(Math.round(x * 1000) / 1000.0)
                .secondaryValue(Math.round(y * 1000) / 1000.0)
                .tertiaryValue(Math.round(z * 1000) / 1000.0)
                .unit("g")
                .quality("GOOD")
                .build();
    }

    /**
     * Temperature: 15-35°C with diurnal pattern, tunnel sensors more stable (18-22°C)
     */
    private SensorReadingRequest generateTemperatureReading(SimulatedSensor sensor) {
        double value;
        
        if (sensor.isInTunnel()) {
            // Tunnel: stable 18-22°C
            value = 20 + random.nextGaussian() * 1.0;
            value = Math.max(18, Math.min(22, value));
        } else {
            // Outside: diurnal pattern 15-35°C
            double hourOfDay = LocalTime.now(ZoneId.systemDefault()).getHour();
            // Peak at 14:00, minimum at 05:00
            double diurnalFactor = Math.sin((hourOfDay - 5) * Math.PI / 12);
            double baseTemp = 25 + diurnalFactor * 8; // 17-33°C base
            value = baseTemp + random.nextGaussian() * 2;
            value = Math.max(15, Math.min(35, value));
        }
        
        return SensorReadingRequest.builder()
                .timestamp(Instant.now())
                .value(Math.round(value * 10) / 10.0)
                .unit("°C")
                .quality("GOOD")
                .build();
    }

    /**
     * Displacement: 0-2mm normal, slow drift over days simulating settlement
     */
    private SensorReadingRequest generateDisplacementReading(SimulatedSensor sensor) {
        // Add very slow drift (settlement simulation)
        sensor.setCurrentDrift(sensor.getCurrentDrift() + random.nextGaussian() * 0.0001);
        sensor.setCurrentDrift(Math.max(0, Math.min(0.5, sensor.getCurrentDrift())));
        
        double value = sensor.getCurrentDrift() + random.nextGaussian() * 0.1;
        value = Math.max(0, Math.min(2, value));
        
        return SensorReadingRequest.builder()
                .timestamp(Instant.now())
                .value(Math.round(value * 100) / 100.0)
                .unit("mm")
                .quality("GOOD")
                .build();
    }

    /**
     * Crack meters: 0.1-0.5mm, very slow growth (0.001mm/day)
     */
    private SensorReadingRequest generateCrackMeterReading(SimulatedSensor sensor) {
        // Very slow crack growth
        if (sensor.getBaselineValue() == 0) {
            sensor.setBaselineValue(0.2 + random.nextDouble() * 0.2); // 0.2-0.4mm initial
        }
        
        // Grow 0.001mm per day = ~0.00001mm per reading (assuming 10s intervals)
        sensor.setBaselineValue(sensor.getBaselineValue() + 0.00001);
        
        double value = sensor.getBaselineValue() + random.nextGaussian() * 0.01;
        value = Math.max(0.1, Math.min(0.5, value));
        
        return SensorReadingRequest.builder()
                .timestamp(Instant.now())
                .value(Math.round(value * 1000) / 1000.0)
                .unit("mm")
                .quality("GOOD")
                .build();
    }

    /**
     * Tiltmeters: 0-0.5 degrees, stable with occasional wind events
     */
    private SensorReadingRequest generateTiltmeterReading(SimulatedSensor sensor) {
        double x = random.nextGaussian() * 0.05; // Very stable base
        double y = random.nextGaussian() * 0.05;
        
        // Occasional wind event (2% chance)
        if (random.nextDouble() < 0.02) {
            double windEffect = 0.2 + random.nextDouble() * 0.2; // 0.2-0.4 degrees
            if (random.nextBoolean()) x += windEffect;
            else y += windEffect;
        }
        
        x = Math.max(-0.5, Math.min(0.5, x));
        y = Math.max(-0.5, Math.min(0.5, y));
        
        return SensorReadingRequest.builder()
                .timestamp(Instant.now())
                .value(Math.round(x * 1000) / 1000.0)
                .secondaryValue(Math.round(y * 1000) / 1000.0)
                .unit("degrees")
                .quality("GOOD")
                .build();
    }

    /**
     * GPS trackers: position with small variations
     */
    private SensorReadingRequest generateGpsReading(SimulatedSensor sensor) {
        // Small GPS drift for position tracking
        double latVariation = random.nextGaussian() * 0.00001;
        double lonVariation = random.nextGaussian() * 0.00001;
        
        return SensorReadingRequest.builder()
                .timestamp(Instant.now())
                .value(latVariation)
                .secondaryValue(lonVariation)
                .unit("degrees")
                .quality("GOOD")
                .build();
    }

    /**
     * Moisture: 30-70% RH, weather-dependent
     */
    private SensorReadingRequest generateMoistureReading(SimulatedSensor sensor) {
        // Base humidity with slow drift (weather simulation)
        if (sensor.getBaselineValue() == 0) {
            sensor.setBaselineValue(50);
        }
        
        // Random walk for weather changes
        sensor.setBaselineValue(sensor.getBaselineValue() + random.nextGaussian() * 0.5);
        sensor.setBaselineValue(Math.max(30, Math.min(70, sensor.getBaselineValue())));
        
        double value = sensor.getBaselineValue() + random.nextGaussian() * 3;
        value = Math.max(30, Math.min(70, value));
        
        return SensorReadingRequest.builder()
                .timestamp(Instant.now())
                .value(Math.round(value * 10) / 10.0)
                .unit("%RH")
                .quality("GOOD")
                .build();
    }

    /**
     * Air quality (tunnel): CO 0-10 ppm, NO2 0-40 ppb, normal with traffic spikes
     */
    private SensorReadingRequest generateAirQualityReading(SimulatedSensor sensor) {
        double hourFactor = getHourlyTrafficFactor();
        
        // CO: 0-10 ppm
        double co = 2 + hourFactor * 5 + random.nextGaussian() * 1;
        co = Math.max(0, Math.min(10, co));
        
        // NO2: 0-40 ppb
        double no2 = 10 + hourFactor * 20 + random.nextGaussian() * 5;
        no2 = Math.max(0, Math.min(40, no2));
        
        return SensorReadingRequest.builder()
                .timestamp(Instant.now())
                .value(Math.round(co * 10) / 10.0)
                .secondaryValue(Math.round(no2 * 10) / 10.0)
                .unit("ppm/ppb")
                .quality("GOOD")
                .build();
    }

    /**
     * Flow sensors for drainage: 0-100 L/min
     */
    private SensorReadingRequest generateFlowSensorReading(SimulatedSensor sensor) {
        // Base flow with weather-influenced variation
        double baseFlow = 20 + random.nextGaussian() * 10;
        
        // Occasional rain event (5% chance for higher flow)
        if (random.nextDouble() < 0.05) {
            baseFlow += 30 + random.nextDouble() * 50;
        }
        
        baseFlow = Math.max(0, Math.min(100, baseFlow));
        
        return SensorReadingRequest.builder()
                .timestamp(Instant.now())
                .value(Math.round(baseFlow * 10) / 10.0)
                .unit("L/min")
                .quality("GOOD")
                .build();
    }

    /**
     * Level sensors for drainage: 0-2m water level
     */
    private SensorReadingRequest generateLevelSensorReading(SimulatedSensor sensor) {
        // Base level with slow variation
        if (sensor.getBaselineValue() == 0) {
            sensor.setBaselineValue(0.3);
        }
        
        sensor.setBaselineValue(sensor.getBaselineValue() + random.nextGaussian() * 0.02);
        sensor.setBaselineValue(Math.max(0.1, Math.min(0.8, sensor.getBaselineValue())));
        
        double value = sensor.getBaselineValue() + random.nextGaussian() * 0.05;
        value = Math.max(0, Math.min(2, value));
        
        return SensorReadingRequest.builder()
                .timestamp(Instant.now())
                .value(Math.round(value * 100) / 100.0)
                .unit("m")
                .quality("GOOD")
                .build();
    }

    /**
     * Impact sensors for guardrails: normally 0, spikes on impact
     */
    private SensorReadingRequest generateImpactSensorReading(SimulatedSensor sensor) {
        double value = 0;
        
        // Very rare impact event (0.1% chance)
        if (random.nextDouble() < 0.001) {
            value = 500 + random.nextDouble() * 1500; // 500-2000 N impact
        }
        
        return SensorReadingRequest.builder()
                .timestamp(Instant.now())
                .value(value)
                .unit("N")
                .quality("GOOD")
                .build();
    }

    /**
     * Default reading generator for unknown sensor types.
     */
    private SensorReadingRequest generateDefaultReading(SimulatedSensor sensor) {
        double value = 50 + random.nextGaussian() * 10;
        
        return SensorReadingRequest.builder()
                .timestamp(Instant.now())
                .value(Math.round(value * 100) / 100.0)
                .unit(sensor.getUnit() != null ? sensor.getUnit() : "unit")
                .quality("GOOD")
                .build();
    }

    /**
     * Apply anomaly effects to a reading.
     */
    private SensorReadingRequest applyAnomaly(SimulatedSensor sensor, SensorReadingRequest reading) {
        AnomalyType anomaly = sensor.getCurrentAnomaly();
        if (anomaly == null) return reading;

        double value = reading.getValue();
        
        switch (anomaly) {
            case GRADUAL_DRIFT -> {
                // Increase drift over time
                sensor.setCurrentDrift(sensor.getCurrentDrift() + 0.5);
                value += sensor.getCurrentDrift();
            }
            case SUDDEN_SPIKE -> {
                // Large sudden increase
                value *= (2 + random.nextDouble());
            }
            case STUCK_VALUE -> {
                // Return same value every time (stuck sensor)
                if (sensor.getBaselineValue() == 0) {
                    sensor.setBaselineValue(value);
                }
                value = sensor.getBaselineValue();
            }
            case ERRATIC_NOISE -> {
                // High variance random noise
                value += random.nextGaussian() * value * 0.5;
            }
        }

        return SensorReadingRequest.builder()
                .timestamp(reading.getTimestamp())
                .value(Math.round(value * 100) / 100.0)
                .unit(reading.getUnit())
                .secondaryValue(reading.getSecondaryValue())
                .tertiaryValue(reading.getTertiaryValue())
                .quality("SUSPECT") // Mark anomalous readings as suspect
                .build();
    }

    /**
     * Get traffic factor based on hour of day (0.0 to 1.0).
     * Peak hours: 7-9 AM and 5-7 PM
     */
    private double getHourlyTrafficFactor() {
        int hour = LocalTime.now(ZoneId.systemDefault()).getHour();
        
        // Morning peak (7-9)
        if (hour >= 7 && hour <= 9) {
            return 0.8 + (hour - 7) * 0.1;
        }
        // Evening peak (17-19)
        if (hour >= 17 && hour <= 19) {
            return 0.8 + (19 - hour) * 0.1;
        }
        // Daytime (10-16)
        if (hour >= 10 && hour <= 16) {
            return 0.5;
        }
        // Night (22-6)
        if (hour >= 22 || hour <= 6) {
            return 0.1;
        }
        // Transition hours
        return 0.3;
    }
}
