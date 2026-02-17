package com.contoso.roadinfra.monitoring.service;

import com.contoso.roadinfra.common.constants.AssetType;
import com.contoso.roadinfra.common.constants.HealthStatus;
import com.contoso.roadinfra.common.exception.ResourceNotFoundException;
import com.contoso.roadinfra.monitoring.dto.AssetHealthResponse;
import com.contoso.roadinfra.monitoring.dto.CorridorHealthSummary;
import com.contoso.roadinfra.monitoring.dto.HealthThresholdResponse;
import com.contoso.roadinfra.monitoring.dto.HealthThresholdUpdateRequest;
import com.contoso.roadinfra.monitoring.entity.AssetHealthRecord;
import com.contoso.roadinfra.monitoring.entity.HealthThreshold;
import com.contoso.roadinfra.monitoring.mapper.MonitoringMapper;
import com.contoso.roadinfra.monitoring.repository.AssetHealthRecordRepository;
import com.contoso.roadinfra.monitoring.repository.HealthThresholdRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for managing asset health monitoring with real-time score calculation.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class HealthMonitoringService {

    private final AssetHealthRecordRepository healthRecordRepository;
    private final HealthThresholdRepository thresholdRepository;
    private final MonitoringMapper mapper;
    private final SimpMessagingTemplate messagingTemplate;

    // ================= Health Status Operations =================

    /**
     * Get the latest health status for an asset.
     */
    @Transactional(readOnly = true)
    @Cacheable(value = "assetHealth", key = "#assetId")
    public AssetHealthResponse getAssetHealth(UUID assetId) {
        log.debug("Getting health status for asset: {}", assetId);
        
        AssetHealthRecord record = healthRecordRepository.findLatestByAssetId(assetId)
                .orElseThrow(() -> new ResourceNotFoundException("Asset health record", assetId));
        
        String trend = calculateTrend(assetId, record.getOverallHealthScore());
        return toHealthResponse(record, trend);
    }

    /**
     * Get health status for all assets with optional filtering.
     */
    @Transactional(readOnly = true)
    public Page<AssetHealthResponse> getAllAssetHealth(AssetType assetType, HealthStatus status, Pageable pageable) {
        log.debug("Getting all asset health records: type={}, status={}", assetType, status);
        
        Page<AssetHealthRecord> records;
        
        if (assetType != null && status != null) {
            records = healthRecordRepository.findLatestByAssetTypeAndStatus(assetType, status, pageable);
        } else if (assetType != null) {
            records = healthRecordRepository.findLatestByAssetType(assetType, pageable);
        } else if (status != null) {
            records = healthRecordRepository.findLatestByStatus(status, pageable);
        } else {
            records = healthRecordRepository.findAllLatest(pageable);
        }
        
        return records.map(r -> toHealthResponse(r, calculateTrend(r.getAssetId(), r.getOverallHealthScore())));
    }

    /**
     * Get health history for a specific asset.
     */
    @Transactional(readOnly = true)
    public Page<AssetHealthResponse> getAssetHealthHistory(UUID assetId, LocalDateTime from, 
                                                            LocalDateTime to, Pageable pageable) {
        log.debug("Getting health history for asset {} from {} to {}", assetId, from, to);
        
        Page<AssetHealthRecord> records = healthRecordRepository.findByAssetIdAndTimestampBetween(
                assetId, from, to, pageable);
        
        return records.map(r -> toHealthResponse(r, null));
    }

    /**
     * Get a summary of health across all assets (corridor view).
     */
    @Transactional(readOnly = true)
    @Cacheable(value = "corridorSummary", key = "'summary'")
    public CorridorHealthSummary getCorridorSummary() {
        log.debug("Computing corridor health summary");
        
        List<AssetHealthRecord> latestRecords = healthRecordRepository.findAllLatestRecords();
        
        if (latestRecords.isEmpty()) {
            return CorridorHealthSummary.builder()
                    .timestamp(LocalDateTime.now())
                    .totalAssets(0)
                    .healthyCount(0)
                    .warningCount(0)
                    .criticalCount(0)
                    .unknownCount(0)
                    .averageHealthScore(0.0)
                    .totalActiveSensors(0)
                    .totalSensors(0)
                    .totalFaultySensors(0)
                    .sensorUptime(0.0)
                    .recentAlertCount(0)
                    .healthByAssetType(Map.of())
                    .build();
        }

        int totalAssets = latestRecords.size();
        
        Map<HealthStatus, Long> statusCounts = latestRecords.stream()
                .collect(Collectors.groupingBy(AssetHealthRecord::getHealthStatus, Collectors.counting()));
        
        double avgScore = latestRecords.stream()
                .mapToDouble(AssetHealthRecord::getOverallHealthScore)
                .average()
                .orElse(0.0);
        
        int totalActiveSensors = latestRecords.stream()
                .mapToInt(r -> r.getActiveSensorCount() != null ? r.getActiveSensorCount() : 0)
                .sum();
        
        int totalSensors = latestRecords.stream()
                .mapToInt(r -> r.getTotalSensorCount() != null ? r.getTotalSensorCount() : 0)
                .sum();
        
        int totalFaultySensors = latestRecords.stream()
                .mapToInt(r -> r.getFaultySensorCount() != null ? r.getFaultySensorCount() : 0)
                .sum();
        
        double sensorUptime = totalSensors > 0 ? (double) totalActiveSensors / totalSensors * 100.0 : 0.0;
        
        int totalAlerts = latestRecords.stream()
                .mapToInt(r -> r.getActiveAlertCount() != null ? r.getActiveAlertCount() : 0)
                .sum();
        
        Map<AssetType, Double> healthByType = latestRecords.stream()
                .collect(Collectors.groupingBy(
                        AssetHealthRecord::getAssetType,
                        Collectors.averagingDouble(AssetHealthRecord::getOverallHealthScore)));

        return CorridorHealthSummary.builder()
                .timestamp(LocalDateTime.now())
                .totalAssets(totalAssets)
                .healthyCount(statusCounts.getOrDefault(HealthStatus.HEALTHY, 0L).intValue())
                .warningCount(statusCounts.getOrDefault(HealthStatus.WARNING, 0L).intValue())
                .criticalCount(statusCounts.getOrDefault(HealthStatus.CRITICAL, 0L).intValue())
                .unknownCount(statusCounts.getOrDefault(HealthStatus.UNKNOWN, 0L).intValue())
                .averageHealthScore(Math.round(avgScore * 10.0) / 10.0)
                .totalActiveSensors(totalActiveSensors)
                .totalSensors(totalSensors)
                .totalFaultySensors(totalFaultySensors)
                .sensorUptime(Math.round(sensorUptime * 10.0) / 10.0)
                .recentAlertCount(totalAlerts)
                .healthByAssetType(healthByType)
                .build();
    }

    // ================= Threshold Operations =================

    /**
     * Get all thresholds.
     */
    @Transactional(readOnly = true)
    public List<HealthThresholdResponse> getAllThresholds() {
        return thresholdRepository.findAll().stream()
                .map(mapper::toThresholdResponse)
                .toList();
    }

    /**
     * Get thresholds for a specific asset type.
     */
    @Transactional(readOnly = true)
    public List<HealthThresholdResponse> getThresholdsByAssetType(AssetType assetType) {
        return thresholdRepository.findByAssetType(assetType).stream()
                .map(mapper::toThresholdResponse)
                .toList();
    }

    /**
     * Create a new threshold.
     */
    @CacheEvict(value = "corridorSummary", allEntries = true)
    public HealthThresholdResponse createThreshold(HealthThresholdUpdateRequest request) {
        log.info("Creating new threshold: {} / {} / {}", 
                request.getAssetType(), request.getSensorType(), request.getMetricName());
        
        HealthThreshold threshold = HealthThreshold.builder()
                .assetType(request.getAssetType())
                .sensorType(request.getSensorType())
                .metricName(request.getMetricName())
                .warningLow(request.getWarningLow())
                .warningHigh(request.getWarningHigh())
                .criticalLow(request.getCriticalLow())
                .criticalHigh(request.getCriticalHigh())
                .unit(request.getUnit())
                .enabled(request.getEnabled() != null ? request.getEnabled() : true)
                .build();
        
        HealthThreshold saved = thresholdRepository.save(threshold);
        return mapper.toThresholdResponse(saved);
    }

    /**
     * Update an existing threshold.
     */
    @CacheEvict(value = "corridorSummary", allEntries = true)
    public HealthThresholdResponse updateThreshold(UUID thresholdId, HealthThresholdUpdateRequest request) {
        log.info("Updating threshold: {}", thresholdId);
        
        HealthThreshold threshold = thresholdRepository.findById(thresholdId)
                .orElseThrow(() -> new ResourceNotFoundException("Health threshold", thresholdId));
        
        mapper.updateThreshold(request, threshold);
        HealthThreshold saved = thresholdRepository.save(threshold);
        
        return mapper.toThresholdResponse(saved);
    }

    /**
     * Delete a threshold.
     */
    @CacheEvict(value = "corridorSummary", allEntries = true)
    public void deleteThreshold(UUID thresholdId) {
        log.info("Deleting threshold: {}", thresholdId);
        
        if (!thresholdRepository.existsById(thresholdId)) {
            throw new ResourceNotFoundException("Health threshold", thresholdId);
        }
        thresholdRepository.deleteById(thresholdId);
    }

    // ================= Helper Methods =================

    /**
     * Convert entity to response DTO.
     */
    public AssetHealthResponse toHealthResponse(AssetHealthRecord record, String trend) {
        return AssetHealthResponse.builder()
                .id(record.getId())
                .assetId(record.getAssetId())
                .assetType(record.getAssetType())
                .timestamp(record.getTimestamp())
                .overallHealthScore(record.getOverallHealthScore())
                .structuralScore(record.getStructuralScore())
                .environmentalScore(record.getEnvironmentalScore())
                .operationalScore(record.getOperationalScore())
                .healthStatus(record.getHealthStatus())
                .activeSensorCount(record.getActiveSensorCount())
                .totalSensorCount(record.getTotalSensorCount())
                .faultySensorCount(record.getFaultySensorCount())
                .activeAlertCount(record.getActiveAlertCount())
                .trend(trend)
                .notes(record.getNotes())
                .build();
    }

    /**
     * Calculate trend based on recent history.
     */
    private String calculateTrend(UUID assetId, Double currentScore) {
        if (currentScore == null) return "STABLE";
        
        List<AssetHealthRecord> recentRecords = healthRecordRepository
                .findTop5ByAssetIdOrderByTimestampDesc(assetId);
        
        if (recentRecords.size() < 2) return "STABLE";
        
        double avgPrevious = recentRecords.stream()
                .skip(1)
                .mapToDouble(AssetHealthRecord::getOverallHealthScore)
                .average()
                .orElse(currentScore);
        
        double diff = currentScore - avgPrevious;
        
        if (diff > 5) return "IMPROVING";
        if (diff < -5) return "DEGRADING";
        return "STABLE";
    }

    /**
     * Broadcast health update via WebSocket.
     */
    public void broadcastHealthUpdate(AssetHealthResponse response) {
        messagingTemplate.convertAndSend("/topic/health-updates", response);
    }

    /**
     * Get assets requiring attention (critical or degrading).
     */
    @Transactional(readOnly = true)
    public List<AssetHealthResponse> getAssetsRequiringAttention() {
        List<AssetHealthRecord> criticalAssets = healthRecordRepository.findLatestByStatus(HealthStatus.CRITICAL);
        
        return criticalAssets.stream()
                .map(r -> toHealthResponse(r, calculateTrend(r.getAssetId(), r.getOverallHealthScore())))
                .toList();
    }
}
