package com.contoso.roadinfra.alert.service;

import com.contoso.roadinfra.alert.entity.Alert;
import com.contoso.roadinfra.alert.mapper.AlertMapper;
import com.contoso.roadinfra.alert.repository.AlertRepository;
import com.contoso.roadinfra.common.constants.AlertSeverity;
import com.contoso.roadinfra.common.dto.AlertDTO;
import com.contoso.roadinfra.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AlertService {

    private final AlertRepository alertRepository;
    private final AlertMapper alertMapper;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final NotificationService notificationService;

    @Transactional(readOnly = true)
    public AlertDTO getAlertById(UUID id) {
        Alert alert = alertRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Alert", id));
        return alertMapper.toDto(alert);
    }

    @Transactional(readOnly = true)
    public Page<AlertDTO> getAllAlerts(Pageable pageable) {
        return alertRepository.findAll(pageable).map(alertMapper::toDto);
    }

    @Transactional(readOnly = true)
    public List<AlertDTO> getActiveAlerts() {
        return alertMapper.toDtoList(alertRepository.findActiveAlerts());
    }

    @Transactional(readOnly = true)
    public List<AlertDTO> getAlertsByAsset(UUID assetId) {
        return alertMapper.toDtoList(alertRepository.findActiveAlertsByAsset(assetId));
    }

    @Transactional(readOnly = true)
    public List<AlertDTO> getUnacknowledgedCriticalAlerts() {
        return alertMapper.toDtoList(alertRepository.findUnacknowledgedAlerts(
                Arrays.asList(AlertSeverity.HIGH, AlertSeverity.CRITICAL)));
    }

    public AlertDTO createAlert(AlertDTO dto) {
        log.info("Creating alert: {} (severity: {})", dto.getTitle(), dto.getSeverity());

        Alert alert = alertMapper.toEntity(dto);
        alert.setStatus("OPEN");
        alert.setAcknowledged(false);
        alert.setResolved(false);
        alert.setTriggeredAt(LocalDateTime.now());

        Alert saved = alertRepository.save(alert);

        AlertDTO savedDto = alertMapper.toDto(saved);
        kafkaTemplate.send("alert-events", "alert.created", savedDto);

        if (dto.getSeverity().isHigherThan(AlertSeverity.LOW)) {
            notificationService.sendNotifications(savedDto);
        }

        return savedDto;
    }

    public AlertDTO acknowledgeAlert(UUID id, UUID userId, String userName) {
        log.info("Acknowledging alert {} by user {}", id, userName);

        Alert alert = alertRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Alert", id));

        alert.setAcknowledged(true);
        alert.setAcknowledgedBy(userId);
        alert.setAcknowledgedByName(userName);
        alert.setAcknowledgedAt(LocalDateTime.now());
        alert.setStatus("ACKNOWLEDGED");

        Alert saved = alertRepository.save(alert);

        kafkaTemplate.send("alert-events", "alert.acknowledged", alertMapper.toDto(saved));

        return alertMapper.toDto(saved);
    }

    public AlertDTO resolveAlert(UUID id, UUID userId, String userName, String resolutionNotes) {
        log.info("Resolving alert {} by user {}", id, userName);

        Alert alert = alertRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Alert", id));

        alert.setResolved(true);
        alert.setResolvedBy(userId);
        alert.setResolvedByName(userName);
        alert.setResolvedAt(LocalDateTime.now());
        alert.setResolutionNotes(resolutionNotes);
        alert.setStatus("RESOLVED");

        Alert saved = alertRepository.save(alert);

        kafkaTemplate.send("alert-events", "alert.resolved", alertMapper.toDto(saved));

        return alertMapper.toDto(saved);
    }

    @KafkaListener(topics = "sensor-anomalies", groupId = "alert-service-group")
    public void handleSensorAnomaly(Map<String, Object> anomaly) {
        log.info("Received sensor anomaly: {}", anomaly);

        AlertDTO alert = AlertDTO.builder()
                .title("Sensor anomaly detected")
                .description("Anomalous reading detected from sensor")
                .severity(AlertSeverity.HIGH)
                .category("ANOMALY")
                .build();

        createAlert(alert);
    }

    @KafkaListener(topics = "health-status-changes", groupId = "alert-service-group")
    public void handleHealthStatusChange(Map<String, Object> statusChange) {
        log.info("Received health status change: {}", statusChange);
        // Create alert based on health status degradation
    }

    @Transactional(readOnly = true)
    public Map<String, Long> getAlertStatistics() {
        return Map.of(
                "critical", alertRepository.countActiveBySeverity(AlertSeverity.CRITICAL),
                "high", alertRepository.countActiveBySeverity(AlertSeverity.HIGH),
                "medium", alertRepository.countActiveBySeverity(AlertSeverity.MEDIUM),
                "low", alertRepository.countActiveBySeverity(AlertSeverity.LOW),
                "info", alertRepository.countActiveBySeverity(AlertSeverity.INFO)
        );
    }
}
