package com.contoso.roadinfra.monitoring.config;

import com.contoso.roadinfra.common.constants.AssetType;
import com.contoso.roadinfra.common.constants.SensorType;
import com.contoso.roadinfra.monitoring.entity.HealthThreshold;
import com.contoso.roadinfra.monitoring.repository.HealthThresholdRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.util.List;

/**
 * Loads default health thresholds on application startup.
 */
@Configuration
@RequiredArgsConstructor
@Slf4j
public class DataLoader {

    private final HealthThresholdRepository thresholdRepository;

    @Bean
    @Profile("!test")
    public CommandLineRunner loadDefaultThresholds() {
        return args -> {
            if (thresholdRepository.count() == 0) {
                log.info("Loading default health thresholds...");
                
                List<HealthThreshold> thresholds = createDefaultThresholds();
                thresholdRepository.saveAll(thresholds);
                
                log.info("Loaded {} default health thresholds", thresholds.size());
            } else {
                log.info("Health thresholds already exist, skipping default load");
            }
        };
    }

    private List<HealthThreshold> createDefaultThresholds() {
        return List.of(
            // Bridge - Structural sensors
            createThreshold(AssetType.BRIDGE, SensorType.STRAIN_GAUGE, "strain", 
                    -500.0, 500.0, -1000.0, 1000.0, "µε"),
            createThreshold(AssetType.BRIDGE, SensorType.ACCELEROMETER, "vibration", 
                    0.0, 0.5, 0.0, 1.0, "g"),
            createThreshold(AssetType.BRIDGE, SensorType.DISPLACEMENT, "displacement", 
                    -10.0, 10.0, -25.0, 25.0, "mm"),
            createThreshold(AssetType.BRIDGE, SensorType.CRACK_SENSOR, "crack_width", 
                    0.0, 0.3, 0.0, 0.5, "mm"),
            
            // Bridge - Environmental sensors
            createThreshold(AssetType.BRIDGE, SensorType.TEMPERATURE, "temperature", 
                    -10.0, 40.0, -20.0, 50.0, "°C"),
            createThreshold(AssetType.BRIDGE, SensorType.HUMIDITY, "humidity", 
                    30.0, 80.0, 20.0, 95.0, "%"),
            
            // Tunnel - Structural sensors
            createThreshold(AssetType.TUNNEL, SensorType.STRAIN_GAUGE, "strain", 
                    -400.0, 400.0, -800.0, 800.0, "µε"),
            createThreshold(AssetType.TUNNEL, SensorType.DISPLACEMENT, "displacement", 
                    -5.0, 5.0, -15.0, 15.0, "mm"),
            createThreshold(AssetType.TUNNEL, SensorType.CRACK_SENSOR, "crack_width", 
                    0.0, 0.2, 0.0, 0.4, "mm"),
            
            // Tunnel - Environmental sensors
            createThreshold(AssetType.TUNNEL, SensorType.TEMPERATURE, "temperature", 
                    5.0, 35.0, 0.0, 45.0, "°C"),
            createThreshold(AssetType.TUNNEL, SensorType.AIR_QUALITY, "co_level", 
                    0.0, 25.0, 0.0, 50.0, "ppm"),
            createThreshold(AssetType.TUNNEL, SensorType.AIR_QUALITY, "visibility", 
                    50.0, 100.0, 20.0, 100.0, "%"),
            
            // Road Section - Sensors
            createThreshold(AssetType.ROAD_SECTION, SensorType.TEMPERATURE, "surface_temp", 
                    -15.0, 55.0, -25.0, 65.0, "°C"),
            createThreshold(AssetType.ROAD_SECTION, SensorType.WEIGHT_IN_MOTION, "axle_load", 
                    0.0, 10000.0, 0.0, 15000.0, "kg"),
            createThreshold(AssetType.ROAD_SECTION, SensorType.TRAFFIC_COUNTER, "traffic_flow", 
                    0.0, 2000.0, 0.0, 3000.0, "veh/hr"),
            
            // Intersection - Sensors
            createThreshold(AssetType.INTERSECTION, SensorType.TRAFFIC_COUNTER, "traffic_flow", 
                    0.0, 1500.0, 0.0, 2500.0, "veh/hr"),
            createThreshold(AssetType.INTERSECTION, SensorType.CCTV, "queue_length", 
                    0.0, 15.0, 0.0, 25.0, "vehicles"),
            
            // Retaining Wall
            createThreshold(AssetType.RETAINING_WALL, SensorType.DISPLACEMENT, "displacement", 
                    -15.0, 15.0, -30.0, 30.0, "mm"),
            createThreshold(AssetType.RETAINING_WALL, SensorType.STRAIN_GAUGE, "strain", 
                    -300.0, 300.0, -600.0, 600.0, "µε"),
            
            // Guardrail
            createThreshold(AssetType.GUARDRAIL, SensorType.ACCELEROMETER, "impact", 
                    0.0, 2.0, 0.0, 5.0, "g")
        );
    }

    private HealthThreshold createThreshold(AssetType assetType, SensorType sensorType, 
                                             String metricName, Double warningLow, 
                                             Double warningHigh, Double criticalLow, 
                                             Double criticalHigh, String unit) {
        return HealthThreshold.builder()
                .assetType(assetType)
                .sensorType(sensorType)
                .metricName(metricName)
                .warningLow(warningLow)
                .warningHigh(warningHigh)
                .criticalLow(criticalLow)
                .criticalHigh(criticalHigh)
                .unit(unit)
                .enabled(true)
                .build();
    }
}
