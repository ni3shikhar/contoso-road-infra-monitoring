package com.contoso.roadinfra.monitoring.service;

import com.contoso.roadinfra.common.constants.HealthStatus;
import com.contoso.roadinfra.common.dto.HealthStatusDTO;
import com.contoso.roadinfra.common.exception.ResourceNotFoundException;
import com.contoso.roadinfra.monitoring.entity.HealthRecord;
import com.contoso.roadinfra.monitoring.repository.HealthRecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class MonitoringService {

    private final HealthRecordRepository healthRecordRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final SimpMessagingTemplate messagingTemplate;

    public HealthStatusDTO recordHealthCheck(UUID assetId, String assetName, HealthStatus status, Integer healthScore, Map<String, Double> metrics) {
        log.info("Recording health check for asset {}: {} (score: {})", assetId, status, healthScore);

        HealthRecord previousRecord = healthRecordRepository.findLatestByAssetId(assetId).orElse(null);
        HealthStatus previousStatus = previousRecord != null ? previousRecord.getStatus() : HealthStatus.UNKNOWN;

        HealthRecord record = HealthRecord.builder()
                .assetId(assetId)
                .assetName(assetName)
                .status(status)
                .previousStatus(previousStatus)
                .healthScore(healthScore)
                .metrics(metrics)
                .statusChanged(!status.equals(previousStatus))
                .trend(calculateTrend(previousRecord, healthScore))
                .checkedAt(LocalDateTime.now())
                .nextCheckAt(LocalDateTime.now().plusMinutes(5))
                .build();

        HealthRecord saved = healthRecordRepository.save(record);

        HealthStatusDTO dto = toDto(saved);

        messagingTemplate.convertAndSend("/topic/health-status/" + assetId, dto);

        if (Boolean.TRUE.equals(record.getStatusChanged())) {
            kafkaTemplate.send("health-status-changes", assetId.toString(), dto);
        }

        return dto;
    }

    @Transactional(readOnly = true)
    public HealthStatusDTO getLatestHealthStatus(UUID assetId) {
        HealthRecord record = healthRecordRepository.findLatestByAssetId(assetId)
                .orElseThrow(() -> new ResourceNotFoundException("Health record", assetId));
        return toDto(record);
    }

    @Transactional(readOnly = true)
    public Page<HealthStatusDTO> getHealthHistory(UUID assetId, Pageable pageable) {
        return healthRecordRepository.findByAssetId(assetId, pageable)
                .map(this::toDto);
    }

    @Transactional(readOnly = true)
    public List<HealthStatusDTO> getRecentStatusChanges() {
        return healthRecordRepository.findStatusChanges(LocalDateTime.now().minusHours(24))
                .stream()
                .map(this::toDto)
                .toList();
    }

    @KafkaListener(topics = "sensor-telemetry", groupId = "monitoring-service-group")
    public void processSensorTelemetry(Map<String, Object> telemetry) {
        log.debug("Received sensor telemetry: {}", telemetry);
        // Process telemetry and update health status as needed
    }

    private String calculateTrend(HealthRecord previous, Integer currentScore) {
        if (previous == null || previous.getHealthScore() == null || currentScore == null) {
            return "STABLE";
        }
        int diff = currentScore - previous.getHealthScore();
        if (diff > 5) return "IMPROVING";
        if (diff < -5) return "DEGRADING";
        return "STABLE";
    }

    private HealthStatusDTO toDto(HealthRecord record) {
        return HealthStatusDTO.builder()
                .id(record.getId())
                .assetId(record.getAssetId())
                .assetName(record.getAssetName())
                .sensorId(record.getSensorId())
                .sensorName(record.getSensorName())
                .status(record.getStatus())
                .previousStatus(record.getPreviousStatus())
                .healthScore(record.getHealthScore())
                .metrics(record.getMetrics())
                .statusChanged(record.getStatusChanged())
                .statusDurationMinutes(record.getStatusDurationMinutes())
                .trend(record.getTrend())
                .checkedAt(record.getCheckedAt())
                .nextCheckAt(record.getNextCheckAt())
                .createdAt(record.getCreatedAt())
                .build();
    }
}
