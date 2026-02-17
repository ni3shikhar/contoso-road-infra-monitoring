package com.contoso.roadinfra.monitoring.kafka;

import com.contoso.roadinfra.common.constants.AssetType;
import com.contoso.roadinfra.common.constants.HealthStatus;
import com.contoso.roadinfra.common.constants.SensorType;
import com.contoso.roadinfra.monitoring.entity.AssetHealthRecord;
import com.contoso.roadinfra.monitoring.entity.HealthThreshold;
import com.contoso.roadinfra.monitoring.repository.AssetHealthRecordRepository;
import com.contoso.roadinfra.monitoring.repository.HealthThresholdRepository;
import com.contoso.roadinfra.monitoring.service.HealthMonitoringService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Consumes sensor readings from Kafka and computes health scores.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SensorReadingConsumer {

    private final AssetHealthRecordRepository healthRecordRepository;
    private final HealthThresholdRepository thresholdRepository;
    private final HealthMonitoringService healthMonitoringService;
    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;

    // Cache of latest sensor readings per asset for aggregation
    private final ConcurrentHashMap<UUID, List<SensorReading>> assetReadingsCache = new ConcurrentHashMap<>();

    // Cache of sensor counts per asset
    private final ConcurrentHashMap<UUID, SensorCounts> sensorCountsCache = new ConcurrentHashMap<>();

    @KafkaListener(topics = "sensor-readings", groupId = "monitoring-service-group",
            containerFactory = "kafkaListenerContainerFactory")
    public void consumeSensorReading(String message) {
        try {
            JsonNode node = objectMapper.readTree(message);
            
            UUID assetId = UUID.fromString(node.get("assetId").asText());
            UUID sensorId = UUID.fromString(node.get("sensorId").asText());
            String sensorTypeStr = node.has("sensorType") ? node.get("sensorType").asText() : "UNKNOWN";
            Double value = node.has("value") ? node.get("value").asDouble() : null;
            String metricName = node.has("metricName") ? node.get("metricName").asText() : "default";

            SensorType sensorType;
            try {
                sensorType = SensorType.valueOf(sensorTypeStr);
            } catch (IllegalArgumentException e) {
                sensorType = SensorType.OTHER;
            }

            SensorReading reading = new SensorReading(sensorId, sensorType, metricName, value, LocalDateTime.now());
            
            assetReadingsCache.computeIfAbsent(assetId, k -> Collections.synchronizedList(new ArrayList<>()))
                    .add(reading);

            // Keep only last 100 readings per asset to prevent memory issues
            List<SensorReading> readings = assetReadingsCache.get(assetId);
            if (readings.size() > 100) {
                readings.subList(0, readings.size() - 100).clear();
            }

            log.debug("Cached sensor reading for asset {}: {} = {}", assetId, metricName, value);
        } catch (Exception e) {
            log.error("Error processing sensor reading: {}", e.getMessage(), e);
        }
    }

    @KafkaListener(topics = "sensor-status-changes", groupId = "monitoring-service-group",
            containerFactory = "kafkaListenerContainerFactory")
    public void consumeSensorStatusChange(String message) {
        try {
            JsonNode node = objectMapper.readTree(message);
            
            UUID assetId = UUID.fromString(node.get("assetId").asText());
            String status = node.has("status") ? node.get("status").asText() : "UNKNOWN";
            
            SensorCounts counts = sensorCountsCache.computeIfAbsent(assetId, k -> new SensorCounts());
            
            switch (status) {
                case "ACTIVE", "ONLINE" -> {
                    counts.active++;
                    counts.total++;
                }
                case "INACTIVE", "OFFLINE" -> {
                    counts.total++;
                }
                case "FAULTY", "ERROR" -> {
                    counts.faulty++;
                    counts.total++;
                }
            }

            log.debug("Updated sensor counts for asset {}: active={}, total={}, faulty={}", 
                    assetId, counts.active, counts.total, counts.faulty);
        } catch (Exception e) {
            log.error("Error processing sensor status change: {}", e.getMessage(), e);
        }
    }

    @KafkaListener(topics = "asset-health-changes", groupId = "monitoring-service-group",
            containerFactory = "kafkaListenerContainerFactory")
    public void consumeAssetHealthChange(String message) {
        try {
            JsonNode node = objectMapper.readTree(message);
            
            UUID assetId = UUID.fromString(node.get("assetId").asText());
            String previousStatus = node.has("previousStatus") ? node.get("previousStatus").asText() : null;
            String newStatus = node.has("newStatus") ? node.get("newStatus").asText() : null;

            log.info("Asset {} health changed: {} -> {}", assetId, previousStatus, newStatus);
            
            // Log the health transition for auditing
        } catch (Exception e) {
            log.error("Error processing asset health change: {}", e.getMessage(), e);
        }
    }

    /**
     * Recompute health scores every 30 seconds based on cached readings.
     */
    @Scheduled(fixedRate = 30000)
    public void recomputeHealthScores() {
        log.debug("Recomputing health scores for {} assets", assetReadingsCache.size());

        for (Map.Entry<UUID, List<SensorReading>> entry : assetReadingsCache.entrySet()) {
            UUID assetId = entry.getKey();
            List<SensorReading> readings = entry.getValue();

            if (readings.isEmpty()) continue;

            try {
                // Get or infer asset type
                AssetType assetType = inferAssetType(readings);
                
                // Compute health scores
                HealthScores scores = computeHealthScores(assetId, assetType, readings);
                
                // Get sensor counts
                SensorCounts counts = sensorCountsCache.getOrDefault(assetId, new SensorCounts());

                // Get previous health record
                Optional<AssetHealthRecord> previousRecord = healthRecordRepository.findLatestByAssetId(assetId);
                HealthStatus previousStatus = previousRecord.map(AssetHealthRecord::getHealthStatus)
                        .orElse(HealthStatus.UNKNOWN);

                // Create new health record
                HealthStatus newStatus = AssetHealthRecord.calculateStatus(scores.overall);
                
                AssetHealthRecord record = AssetHealthRecord.builder()
                        .assetId(assetId)
                        .assetType(assetType)
                        .timestamp(LocalDateTime.now())
                        .overallHealthScore(scores.overall)
                        .structuralScore(scores.structural)
                        .environmentalScore(scores.environmental)
                        .operationalScore(scores.operational)
                        .healthStatus(newStatus)
                        .activeSensorCount(counts.active)
                        .totalSensorCount(counts.total)
                        .faultySensorCount(counts.faulty)
                        .activeAlertCount(0) // Will be updated from alert-service
                        .build();

                AssetHealthRecord saved = healthRecordRepository.save(record);

                // Broadcast update via WebSocket
                messagingTemplate.convertAndSend("/topic/health-updates", 
                        healthMonitoringService.toHealthResponse(saved, null));

                // If status changed, notify
                if (previousStatus != newStatus) {
                    log.info("Asset {} health status changed: {} -> {} (score: {})", 
                            assetId, previousStatus, newStatus, scores.overall);
                }

            } catch (Exception e) {
                log.error("Error computing health scores for asset {}: {}", assetId, e.getMessage(), e);
            }
        }
    }

    private HealthScores computeHealthScores(UUID assetId, AssetType assetType, List<SensorReading> readings) {
        List<HealthThreshold> thresholds = thresholdRepository.findByAssetTypeAndEnabledTrue(assetType);
        
        double structuralTotal = 0, structuralCount = 0;
        double environmentalTotal = 0, environmentalCount = 0;
        double operationalTotal = 0, operationalCount = 0;

        for (SensorReading reading : readings) {
            // Find applicable threshold
            Optional<HealthThreshold> thresholdOpt = thresholds.stream()
                    .filter(t -> t.getSensorType() == reading.sensorType && 
                                t.getMetricName().equals(reading.metricName))
                    .findFirst();

            double score = thresholdOpt.map(t -> t.evaluateScore(reading.value)).orElse(75.0);

            // Categorize by sensor type
            switch (reading.sensorType) {
                case STRAIN_GAUGE, ACCELEROMETER, DISPLACEMENT, CRACK_SENSOR -> {
                    structuralTotal += score;
                    structuralCount++;
                }
                case TEMPERATURE, HUMIDITY, WEATHER_STATION, AIR_QUALITY -> {
                    environmentalTotal += score;
                    environmentalCount++;
                }
                case TRAFFIC_COUNTER, WEIGHT_IN_MOTION, CCTV -> {
                    operationalTotal += score;
                    operationalCount++;
                }
                default -> {
                    // Add to operational by default
                    operationalTotal += score;
                    operationalCount++;
                }
            }
        }

        double structural = structuralCount > 0 ? structuralTotal / structuralCount : 75.0;
        double environmental = environmentalCount > 0 ? environmentalTotal / environmentalCount : 75.0;
        double operational = operationalCount > 0 ? operationalTotal / operationalCount : 75.0;

        // Weighted average for overall score
        // Structural: 50%, Environmental: 25%, Operational: 25%
        double overall = (structural * 0.5) + (environmental * 0.25) + (operational * 0.25);

        return new HealthScores(overall, structural, environmental, operational);
    }

    private AssetType inferAssetType(List<SensorReading> readings) {
        // Simple heuristic based on sensor types present
        boolean hasStructural = readings.stream().anyMatch(r -> 
                r.sensorType == SensorType.STRAIN_GAUGE || r.sensorType == SensorType.DISPLACEMENT);
        boolean hasEnvironmental = readings.stream().anyMatch(r ->
                r.sensorType == SensorType.TEMPERATURE || r.sensorType == SensorType.HUMIDITY);
        
        if (hasStructural && hasEnvironmental) {
            return AssetType.BRIDGE;
        } else if (hasStructural) {
            return AssetType.TUNNEL;
        }
        return AssetType.ROAD_SECTION;
    }

    // Internal data classes
    private record SensorReading(UUID sensorId, SensorType sensorType, String metricName, 
                                  Double value, LocalDateTime timestamp) {}
    
    private record HealthScores(double overall, double structural, double environmental, double operational) {}
    
    private static class SensorCounts {
        int active = 0;
        int total = 0;
        int faulty = 0;
    }
}
