package com.contoso.roadinfra.sensor.config;

import com.contoso.roadinfra.common.constants.AssetType;
import com.contoso.roadinfra.common.constants.SensorStatus;
import com.contoso.roadinfra.common.constants.SensorType;
import com.contoso.roadinfra.sensor.entity.Sensor;
import com.contoso.roadinfra.sensor.repository.SensorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

/**
 * Data loader for seeding the database with sample sensors for a 2km corridor.
 * Sensors are distributed across road sections, bridge, and tunnel.
 */
@Component
@Profile({"dev", "local", "default", "docker"})
@RequiredArgsConstructor
@Slf4j
public class DataLoader implements CommandLineRunner {

    private final SensorRepository sensorRepository;
    
    // Corridor base coordinates (Seattle area - same as asset-service)
    private static final double BASE_LAT = 47.6062;
    private static final double BASE_LON = -122.3321;
    private static final double KM_TO_LAT = 0.009;
    private static final double KM_TO_LON = 0.012;
    
    private final Random random = new Random(42); // Fixed seed for reproducibility

    @Override
    @Transactional
    public void run(String... args) {
        if (sensorRepository.count() > 0) {
            log.info("Sensors already exist, skipping seed data");
            return;
        }

        log.info("Seeding sensor database with corridor sensors...");
        
        List<Sensor> sensors = new ArrayList<>();
        
        // Road Section sensors (3 sections, multiple sensors each)
        // Section A: km 0.0-0.4
        UUID roadSection1Id = UUID.randomUUID();
        sensors.addAll(createRoadSectionSensors("RS-001", roadSection1Id, 0.0, 0.4, AssetType.ROAD));
        
        // Section B: km 0.4-0.8
        UUID roadSection2Id = UUID.randomUUID();
        sensors.addAll(createRoadSectionSensors("RS-002", roadSection2Id, 0.4, 0.8, AssetType.ROAD));
        
        // Section C: km 0.8-1.2
        UUID roadSection3Id = UUID.randomUUID();
        sensors.addAll(createRoadSectionSensors("RS-003", roadSection3Id, 0.8, 1.2, AssetType.ROAD));
        
        // Bridge sensors (km 1.2-1.5)
        UUID bridgeId = UUID.randomUUID();
        sensors.addAll(createBridgeSensors("BR-001", bridgeId, 1.2, 1.5));
        
        // Tunnel sensors (km 1.5-1.8)
        UUID tunnelId = UUID.randomUUID();
        sensors.addAll(createTunnelSensors("TN-001", tunnelId, 1.5, 1.8));
        
        // Drainage sensors
        UUID drainage1Id = UUID.randomUUID();
        sensors.addAll(createDrainageSensors("DR-001", drainage1Id, 0.5));
        
        UUID drainage2Id = UUID.randomUUID();
        sensors.addAll(createDrainageSensors("DR-002", drainage2Id, 1.0));
        
        sensorRepository.saveAll(sensors);
        log.info("Seeded {} sensors for the 2km corridor", sensors.size());
    }
    
    private List<Sensor> createRoadSectionSensors(String assetCode, UUID assetId, double startKm, double endKm, AssetType assetType) {
        List<Sensor> sensors = new ArrayList<>();
        double midKm = (startKm + endKm) / 2;
        
        // Strain gauge at center
        sensors.add(createSensor(assetCode + "-STR-01", SensorType.STRAIN_GAUGE, assetId, assetType,
                midKm, "Strain sensor on road surface", "StrainTech", "ST-2000", 
                -500.0, 500.0, "microstrain"));
        
        // Temperature sensor
        sensors.add(createSensor(assetCode + "-TMP-01", SensorType.TEMPERATURE, assetId, assetType,
                startKm + 0.1, "Pavement temperature sensor", "ThermoSense", "TS-100",
                -40.0, 80.0, "°C"));
        
        // Accelerometer for vibration monitoring
        sensors.add(createSensor(assetCode + "-ACC-01", SensorType.ACCELEROMETER, assetId, assetType,
                midKm, "Road vibration monitor", "VibroTech", "VT-500",
                0.0, 10.0, "g"));
        
        // Traffic counter
        sensors.add(createSensor(assetCode + "-TRF-01", SensorType.TRAFFIC_COUNTER, assetId, assetType,
                endKm - 0.05, "Traffic volume counter", "TrafficSys", "TC-300",
                0.0, 10000.0, "vehicles/hour"));
        
        // Weather station (only on first and last section)
        if (startKm < 0.1 || endKm > 1.1) {
            sensors.add(createSensor(assetCode + "-WTH-01", SensorType.WEATHER_STATION, assetId, assetType,
                    midKm, "Weather monitoring station", "WeatherPro", "WP-450",
                    null, null, null));
        }
        
        return sensors;
    }
    
    private List<Sensor> createBridgeSensors(String assetCode, UUID assetId, double startKm, double endKm) {
        List<Sensor> sensors = new ArrayList<>();
        double length = endKm - startKm;
        
        // Multiple strain gauges along the bridge
        sensors.add(createSensor(assetCode + "-STR-01", SensorType.STRAIN_GAUGE, assetId, AssetType.BRIDGE,
                startKm + length * 0.25, "Bridge deck strain - north quarter", "StrainTech", "ST-3000",
                -1000.0, 1000.0, "microstrain"));
        sensors.add(createSensor(assetCode + "-STR-02", SensorType.STRAIN_GAUGE, assetId, AssetType.BRIDGE,
                startKm + length * 0.5, "Bridge deck strain - center", "StrainTech", "ST-3000",
                -1000.0, 1000.0, "microstrain"));
        sensors.add(createSensor(assetCode + "-STR-03", SensorType.STRAIN_GAUGE, assetId, AssetType.BRIDGE,
                startKm + length * 0.75, "Bridge deck strain - south quarter", "StrainTech", "ST-3000",
                -1000.0, 1000.0, "microstrain"));
        
        // Accelerometers on piers
        sensors.add(createSensor(assetCode + "-ACC-01", SensorType.ACCELEROMETER, assetId, AssetType.BRIDGE,
                startKm + length * 0.33, "North pier accelerometer", "AccelTech", "AT-200",
                -10.0, 10.0, "g"));
        sensors.add(createSensor(assetCode + "-ACC-02", SensorType.ACCELEROMETER, assetId, AssetType.BRIDGE,
                startKm + length * 0.66, "South pier accelerometer", "AccelTech", "AT-200",
                -10.0, 10.0, "g"));
        
        // Displacement sensors
        sensors.add(createSensor(assetCode + "-DIS-01", SensorType.DISPLACEMENT, assetId, AssetType.BRIDGE,
                startKm, "Expansion joint displacement - north", "DisplaceTech", "DT-100",
                -50.0, 50.0, "mm"));
        sensors.add(createSensor(assetCode + "-DIS-02", SensorType.DISPLACEMENT, assetId, AssetType.BRIDGE,
                endKm, "Expansion joint displacement - south", "DisplaceTech", "DT-100",
                -50.0, 50.0, "mm"));
        
        // Tilt sensors
        sensors.add(createSensor(assetCode + "-TLT-01", SensorType.TILTMETER, assetId, AssetType.BRIDGE,
                startKm + length * 0.5, "Bridge deck tilt sensor", "TiltMaster", "TM-50",
                -5.0, 5.0, "degrees"));
        
        // Moisture sensor for corrosion monitoring
        sensors.add(createSensor(assetCode + "-MST-01", SensorType.MOISTURE, assetId, AssetType.BRIDGE,
                startKm + length * 0.5, "Rebar corrosion monitor", "CorroSense", "CS-100",
                0.0, 100.0, "%"));
        
        // Temperature
        sensors.add(createSensor(assetCode + "-TMP-01", SensorType.TEMPERATURE, assetId, AssetType.BRIDGE,
                startKm + length * 0.5, "Bridge deck temperature", "ThermoSense", "TS-200",
                -40.0, 80.0, "°C"));
        
        // Weather station for wind monitoring
        sensors.add(createSensor(assetCode + "-WTH-01", SensorType.WEATHER_STATION, assetId, AssetType.BRIDGE,
                startKm + length * 0.5, "Bridge weather station", "WindTech", "WT-100",
                null, null, null));
        
        return sensors;
    }
    
    private List<Sensor> createTunnelSensors(String assetCode, UUID assetId, double startKm, double endKm) {
        List<Sensor> sensors = new ArrayList<>();
        double length = endKm - startKm;
        
        // Air quality sensors
        sensors.add(createSensor(assetCode + "-AIR-01", SensorType.AIR_QUALITY, assetId, AssetType.TUNNEL,
                startKm + 0.05, "Tunnel entry air quality", "AirSense", "AQ-500",
                0.0, 500.0, "AQI"));
        sensors.add(createSensor(assetCode + "-AIR-02", SensorType.AIR_QUALITY, assetId, AssetType.TUNNEL,
                startKm + length * 0.5, "Tunnel center air quality", "AirSense", "AQ-500",
                0.0, 500.0, "AQI"));
        sensors.add(createSensor(assetCode + "-AIR-03", SensorType.AIR_QUALITY, assetId, AssetType.TUNNEL,
                endKm - 0.05, "Tunnel exit air quality", "AirSense", "AQ-500",
                0.0, 500.0, "AQI"));
        
        // Temperature sensors
        sensors.add(createSensor(assetCode + "-TMP-01", SensorType.TEMPERATURE, assetId, AssetType.TUNNEL,
                startKm + length * 0.25, "Tunnel temperature - section 1", "ThermoSense", "TS-150",
                -20.0, 60.0, "°C"));
        sensors.add(createSensor(assetCode + "-TMP-02", SensorType.TEMPERATURE, assetId, AssetType.TUNNEL,
                startKm + length * 0.5, "Tunnel temperature - section 2", "ThermoSense", "TS-150",
                -20.0, 60.0, "°C"));
        sensors.add(createSensor(assetCode + "-TMP-03", SensorType.TEMPERATURE, assetId, AssetType.TUNNEL,
                startKm + length * 0.75, "Tunnel temperature - section 3", "ThermoSense", "TS-150",
                -20.0, 60.0, "°C"));
        
        // Humidity sensors
        sensors.add(createSensor(assetCode + "-HUM-01", SensorType.HUMIDITY, assetId, AssetType.TUNNEL,
                startKm + length * 0.5, "Tunnel humidity sensor", "HumidiTech", "HT-200",
                0.0, 100.0, "%RH"));
        
        // Crack sensors
        sensors.add(createSensor(assetCode + "-CRK-01", SensorType.CRACK_SENSOR, assetId, AssetType.TUNNEL,
                startKm + length * 0.3, "Tunnel wall crack sensor - section 1", "CrackWatch", "CW-50",
                0.0, 10.0, "mm"));
        sensors.add(createSensor(assetCode + "-CRK-02", SensorType.CRACK_SENSOR, assetId, AssetType.TUNNEL,
                startKm + length * 0.6, "Tunnel wall crack sensor - section 2", "CrackWatch", "CW-50",
                0.0, 10.0, "mm"));
        
        // Convergence sensors (tunnel deformation)
        sensors.add(createSensor(assetCode + "-CVG-01", SensorType.DISPLACEMENT, assetId, AssetType.TUNNEL,
                startKm + length * 0.5, "Tunnel convergence monitor", "ConvergeSense", "CS-300",
                -100.0, 100.0, "mm"));
        
        // CCTV (status only)
        sensors.add(createSensor(assetCode + "-CAM-01", SensorType.CCTV, assetId, AssetType.TUNNEL,
                startKm + 0.02, "Tunnel entry camera", "VisionPro", "VP-4K",
                null, null, null));
        sensors.add(createSensor(assetCode + "-CAM-02", SensorType.CCTV, assetId, AssetType.TUNNEL,
                endKm - 0.02, "Tunnel exit camera", "VisionPro", "VP-4K",
                null, null, null));
        
        return sensors;
    }
    
    private List<Sensor> createDrainageSensors(String assetCode, UUID assetId, double km) {
        List<Sensor> sensors = new ArrayList<>();
        
        // Moisture sensor for water level
        sensors.add(createSensor(assetCode + "-MST-01", SensorType.MOISTURE, assetId, AssetType.DRAINAGE,
                km, "Drainage water level", "HydroSense", "WL-200",
                0.0, 100.0, "%"));
        
        // Displacement sensor for flow monitoring
        sensors.add(createSensor(assetCode + "-DIS-01", SensorType.DISPLACEMENT, assetId, AssetType.DRAINAGE,
                km, "Drainage flow sensor", "FlowTech", "FR-100",
                0.0, 100.0, "mm"));
        
        return sensors;
    }
    
    private Sensor createSensor(String code, SensorType type, UUID assetId, AssetType assetType,
                                 double km, String description, String manufacturer, String model,
                                 Double minThreshold, Double maxThreshold, String unit) {
        double lat = BASE_LAT + (km * KM_TO_LAT);
        double lon = BASE_LON + (km * KM_TO_LON * 0.3); // Slight offset for corridor angle
        double elevation = 50.0 + (random.nextDouble() * 20 - 10); // Base elevation with variance
        
        LocalDate installDate = LocalDate.now().minusMonths(random.nextInt(24) + 1);
        LocalDate calibrationDate = installDate.plusMonths(random.nextInt(6));
        
        return Sensor.builder()
                .sensorCode(code)
                .sensorType(type)
                .assetId(assetId)
                .assetType(assetType)
                .latitude(lat)
                .longitude(lon)
                .elevation(elevation)
                .locationDescription(description)
                .manufacturer(manufacturer)
                .model(model)
                .firmwareVersion("v" + (1 + random.nextInt(3)) + "." + random.nextInt(10) + "." + random.nextInt(10))
                .installationDate(installDate)
                .lastCalibrationDate(calibrationDate)
                .calibrationIntervalDays(180)
                .status(SensorStatus.ACTIVE)
                .batteryLevel(70.0 + random.nextDouble() * 30) // 70-100%
                .signalStrength(-50.0 - random.nextDouble() * 30) // -50 to -80 dBm
                .minThreshold(minThreshold)
                .maxThreshold(maxThreshold)
                .unit(unit)
                .build();
    }
}
