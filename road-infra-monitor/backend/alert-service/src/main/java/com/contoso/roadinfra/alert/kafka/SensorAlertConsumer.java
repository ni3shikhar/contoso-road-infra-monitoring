package com.contoso.roadinfra.alert.kafka;

import com.contoso.roadinfra.alert.constants.AlertStatus;
import com.contoso.roadinfra.alert.constants.SourceType;
import com.contoso.roadinfra.alert.entity.Alert;
import com.contoso.roadinfra.alert.entity.AlertRule;
import com.contoso.roadinfra.alert.repository.AlertRepository;
import com.contoso.roadinfra.alert.repository.AlertRuleRepository;
import com.contoso.roadinfra.alert.service.EscalationService;
import com.contoso.roadinfra.alert.service.NotificationService;
import com.contoso.roadinfra.common.constants.AlertSeverity;
import com.contoso.roadinfra.common.constants.AssetType;
import com.contoso.roadinfra.common.constants.SensorType;
import com.contoso.roadinfra.common.dto.AlertDTO;
import com.contoso.roadinfra.alert.mapper.AlertMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Kafka consumer for sensor alerts and related events.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SensorAlertConsumer {

    private final AlertRepository alertRepository;
    private final AlertRuleRepository ruleRepository;
    private final AlertMapper alertMapper;
    private final NotificationService notificationService;
    private final EscalationService escalationService;
    private final SimpMessagingTemplate messagingTemplate;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "sensor-alerts", groupId = "alert-service-group",
            containerFactory = "kafkaListenerContainerFactory")
    public void consumeSensorAlert(String message) {
        try {
            JsonNode node = objectMapper.readTree(message);
            
            UUID sensorId = UUID.fromString(node.get("sensorId").asText());
            UUID assetId = UUID.fromString(node.get("assetId").asText());
            String sensorName = node.has("sensorName") ? node.get("sensorName").asText() : "Unknown Sensor";
            String assetName = node.has("assetName") ? node.get("assetName").asText() : "Unknown Asset";
            Double value = node.has("value") ? node.get("value").asDouble() : null;
            String metricName = node.has("metricName") ? node.get("metricName").asText() : "value";
            String sensorTypeStr = node.has("sensorType") ? node.get("sensorType").asText() : null;
            String assetTypeStr = node.has("assetType") ? node.get("assetType").asText() : null;
            
            SensorType sensorType = parseSensorType(sensorTypeStr);
            AssetType assetType = parseAssetType(assetTypeStr);

            log.debug("Received sensor alert: sensor={}, asset={}, value={}", sensorId, assetId, value);

            // Find matching rules
            List<AlertRule> rules = ruleRepository.findMatchingRules(assetType, sensorType, metricName);

            for (AlertRule rule : rules) {
                if (rule.evaluate(value)) {
                    processRuleMatch(rule, sensorId, assetId, sensorName, assetName, value);
                } else {
                    // Check for auto-resolve
                    checkAutoResolve(rule, sensorId);
                }
            }
        } catch (Exception e) {
            log.error("Error processing sensor alert: {}", e.getMessage(), e);
        }
    }

    @KafkaListener(topics = "asset-health-changes", groupId = "alert-service-group",
            containerFactory = "kafkaListenerContainerFactory")
    public void consumeHealthChange(String message) {
        try {
            JsonNode node = objectMapper.readTree(message);
            
            UUID assetId = UUID.fromString(node.get("assetId").asText());
            String assetName = node.has("assetName") ? node.get("assetName").asText() : "Unknown Asset";
            String previousStatus = node.has("previousStatus") ? node.get("previousStatus").asText() : null;
            String newStatus = node.has("newStatus") ? node.get("newStatus").asText() : null;
            Double healthScore = node.has("healthScore") ? node.get("healthScore").asDouble() : null;

            log.info("Asset {} health changed: {} -> {}", assetId, previousStatus, newStatus);

            // Create alert for critical health changes
            if ("CRITICAL".equals(newStatus) && !"CRITICAL".equals(previousStatus)) {
                createHealthAlert(assetId, assetName, healthScore, AlertSeverity.CRITICAL);
            } else if ("WARNING".equals(newStatus) && "HEALTHY".equals(previousStatus)) {
                createHealthAlert(assetId, assetName, healthScore, AlertSeverity.MEDIUM);
            }
        } catch (Exception e) {
            log.error("Error processing health change: {}", e.getMessage(), e);
        }
    }

    private void processRuleMatch(AlertRule rule, UUID sensorId, UUID assetId, 
                                   String sensorName, String assetName, Double value) {
        // Check cooldown
        LocalDateTime cooldownCutoff = LocalDateTime.now().minusMinutes(rule.getCooldownMinutes());
        Optional<Alert> recentAlert = alertRepository.findRecentByRuleAndAsset(
                rule.getId(), assetId, cooldownCutoff);

        if (recentAlert.isPresent()) {
            log.debug("Skipping alert for rule {} - in cooldown period", rule.getCode());
            return;
        }

        // Create new alert
        Alert alert = Alert.builder()
                .alertCode(rule.getCode())
                .ruleId(rule.getId())
                .title(rule.generateTitle(sensorName, assetName, value))
                .description(rule.generateDescription(sensorName, assetName, value))
                .severity(rule.getSeverity())
                .originalSeverity(rule.getSeverity())
                .sourceType(SourceType.SENSOR)
                .category(rule.getCategory())
                .assetId(assetId)
                .assetName(assetName)
                .sensorId(sensorId)
                .sensorName(sensorName)
                .triggerValue(value)
                .thresholdValue(rule.getThresholdValue())
                .unit(rule.getUnit())
                .alertStatus(AlertStatus.OPEN)
                .status("OPEN")
                .escalationLevel(0)
                .triggeredAt(LocalDateTime.now())
                .build();

        Alert saved = alertRepository.save(alert);
        log.info("Created alert: {} (code: {}, severity: {})", saved.getId(), rule.getCode(), rule.getSeverity());

        // Broadcast via WebSocket
        AlertDTO dto = alertMapper.toDto(saved);
        messagingTemplate.convertAndSend("/topic/alerts", dto);

        // Send Kafka event
        kafkaTemplate.send("alert-events", "alert.created", dto);

        // Send notifications for high-severity alerts
        if (rule.getSeverity().isHigherThan(AlertSeverity.LOW)) {
            notificationService.sendNotifications(dto);
        }

        // Schedule escalation if configured
        if (rule.getEscalationMinutes() != null) {
            escalationService.scheduleEscalation(saved.getId(), rule.getEscalationMinutes());
        }
    }

    private void createHealthAlert(UUID assetId, String assetName, Double healthScore, AlertSeverity severity) {
        Alert alert = Alert.builder()
                .alertCode("HEALTH_STATUS_CHANGE")
                .title(String.format("Asset health %s: %s", severity == AlertSeverity.CRITICAL ? "critical" : "degraded", assetName))
                .description(String.format("Asset %s health score dropped to %.1f", assetName, healthScore))
                .severity(severity)
                .originalSeverity(severity)
                .sourceType(SourceType.HEALTH)
                .category("HEALTH")
                .assetId(assetId)
                .assetName(assetName)
                .triggerValue(healthScore)
                .alertStatus(AlertStatus.OPEN)
                .status("OPEN")
                .escalationLevel(0)
                .triggeredAt(LocalDateTime.now())
                .build();

        Alert saved = alertRepository.save(alert);
        
        AlertDTO dto = alertMapper.toDto(saved);
        messagingTemplate.convertAndSend("/topic/alerts", dto);
        kafkaTemplate.send("alert-events", "alert.created", dto);
        notificationService.sendNotifications(dto);
    }

    private void checkAutoResolve(AlertRule rule, UUID sensorId) {
        if (Boolean.TRUE.equals(rule.getAutoResolve())) {
            List<Alert> activeAlerts = alertRepository.findActiveAlertsBySensorAndCode(sensorId, rule.getCode());
            for (Alert alert : activeAlerts) {
                alert.setResolved(true);
                alert.setAlertStatus(AlertStatus.AUTO_RESOLVED);
                alert.setStatus("AUTO_RESOLVED");
                alert.setResolvedAt(LocalDateTime.now());
                alert.setResolutionNotes("Auto-resolved: condition cleared");
                alertRepository.save(alert);
                
                log.info("Auto-resolved alert: {}", alert.getId());
                
                AlertDTO dto = alertMapper.toDto(alert);
                messagingTemplate.convertAndSend("/topic/alerts", dto);
                kafkaTemplate.send("alert-events", "alert.resolved", dto);
            }
        }
    }

    private SensorType parseSensorType(String str) {
        if (str == null) return null;
        try {
            return SensorType.valueOf(str);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private AssetType parseAssetType(String str) {
        if (str == null) return null;
        try {
            return AssetType.valueOf(str);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
