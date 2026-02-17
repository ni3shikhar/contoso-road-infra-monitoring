package com.contoso.roadinfra.analytics.service;

import com.contoso.roadinfra.analytics.client.AlertServiceClient;
import com.contoso.roadinfra.analytics.client.AssetServiceClient;
import com.contoso.roadinfra.analytics.client.MonitoringServiceClient;
import com.contoso.roadinfra.analytics.client.SensorServiceClient;
import com.contoso.roadinfra.analytics.constants.Trend;
import com.contoso.roadinfra.analytics.entity.KpiSnapshot;
import com.contoso.roadinfra.analytics.repository.KpiSnapshotRepository;
import com.contoso.roadinfra.common.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Service for calculating and tracking KPIs.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class KpiCalculationService {

    private final KpiSnapshotRepository snapshotRepository;
    private final SensorServiceClient sensorServiceClient;
    private final AssetServiceClient assetServiceClient;
    private final MonitoringServiceClient monitoringServiceClient;
    private final AlertServiceClient alertServiceClient;

    // KPI Constants
    public static final String KPI_SENSOR_UPTIME = "sensor_uptime";
    public static final String KPI_MTBF = "mtbf";
    public static final String KPI_HEALTH_INDEX = "corridor_health_index";
    public static final String KPI_ALERT_RESPONSE_TIME = "alert_response_time";
    public static final String KPI_CONSTRUCTION_PROGRESS = "construction_progress";
    public static final String KPI_SPI = "schedule_performance_index";
    public static final String KPI_DATA_QUALITY = "data_quality";
    public static final String KPI_SENSOR_COVERAGE = "sensor_coverage";
    public static final String KPI_CRITICAL_ALERTS = "critical_alerts_rate";
    public static final String KPI_PREDICTIVE_ACCURACY = "predictive_accuracy";
    public static final String KPI_ASSET_UTILIZATION = "asset_utilization";
    public static final String KPI_MAINTENANCE_EFFICIENCY = "maintenance_efficiency";

    /**
     * Calculate all KPIs - scheduled daily at 1 AM.
     */
    @Scheduled(cron = "0 0 1 * * *")
    public void calculateDailyKpis() {
        log.info("Starting daily KPI calculation");
        LocalDate today = LocalDate.now();

        try {
            // Sensor-related KPIs
            calculateSensorUptime(today);
            calculateMTBF(today);
            calculateSensorCoverage(today);
            calculateDataQuality(today);

            // Health-related KPIs
            calculateCorridorHealthIndex(today);
            
            // Alert-related KPIs
            calculateAlertResponseTime(today);
            calculateCriticalAlertsRate(today);
            
            // Asset-related KPIs
            calculateAssetUtilization(today);
            
            // Project KPIs (simulated)
            calculateConstructionProgress(today);
            calculateSchedulePerformanceIndex(today);
            
            // Quality KPIs
            calculatePredictiveAccuracy(today);
            calculateMaintenanceEfficiency(today);

            log.info("Daily KPI calculation completed");
        } catch (Exception e) {
            log.error("Error during daily KPI calculation: {}", e.getMessage(), e);
        }
    }

    /**
     * Calculate sensor uptime percentage.
     */
    private void calculateSensorUptime(LocalDate date) {
        try {
            ApiResponse<Map<String, Long>> response = sensorServiceClient.getSensorStatusCounts();
            Map<String, Long> counts = response.getData();
            
            long active = counts.getOrDefault("ACTIVE", 0L);
            long total = counts.values().stream().mapToLong(Long::longValue).sum();
            
            double uptime = total > 0 ? (double) active / total * 100.0 : 0.0;
            
            saveSnapshot(KPI_SENSOR_UPTIME, "Sensor Uptime", "Operations", 
                    date, uptime, 98.0, "%");
            
            log.debug("Sensor uptime calculated: {}%", uptime);
        } catch (Exception e) {
            log.error("Error calculating sensor uptime: {}", e.getMessage());
        }
    }

    /**
     * Calculate Mean Time Between Failures (hours).
     */
    private void calculateMTBF(LocalDate date) {
        try {
            ApiResponse<Map<String, Object>> response = sensorServiceClient.getSensorStatistics();
            Map<String, Object> stats = response.getData();
            
            // MTBF calculation based on sensor statistics
            // Simulated: total operational hours / number of failures
            double mtbf = stats.containsKey("mtbf") 
                    ? ((Number) stats.get("mtbf")).doubleValue() 
                    : 720.0; // Default 30 days
            
            saveSnapshot(KPI_MTBF, "Mean Time Between Failures", "Reliability", 
                    date, mtbf, 500.0, "hours");
            
            log.debug("MTBF calculated: {} hours", mtbf);
        } catch (Exception e) {
            log.error("Error calculating MTBF: {}", e.getMessage());
        }
    }

    /**
     * Calculate sensor coverage percentage.
     */
    private void calculateSensorCoverage(LocalDate date) {
        try {
            ApiResponse<Map<String, Object>> assetStats = assetServiceClient.getAssetStatistics();
            ApiResponse<Map<String, Object>> sensorStats = sensorServiceClient.getSensorStatistics();
            
            int totalAssets = assetStats.getData().containsKey("totalAssets")
                    ? ((Number) assetStats.getData().get("totalAssets")).intValue() : 0;
            int totalSensors = sensorStats.getData().containsKey("totalSensors")
                    ? ((Number) sensorStats.getData().get("totalSensors")).intValue() : 0;
            
            // Assuming minimum 5 sensors per asset for full coverage
            double coverage = totalAssets > 0 
                    ? Math.min(100.0, (double) totalSensors / (totalAssets * 5) * 100.0) 
                    : 0.0;
            
            saveSnapshot(KPI_SENSOR_COVERAGE, "Sensor Coverage", "Infrastructure", 
                    date, coverage, 95.0, "%");
            
            log.debug("Sensor coverage calculated: {}%", coverage);
        } catch (Exception e) {
            log.error("Error calculating sensor coverage: {}", e.getMessage());
        }
    }

    /**
     * Calculate data quality score.
     */
    private void calculateDataQuality(LocalDate date) {
        try {
            // Data quality is based on:
            // - Sensor uptime (40%)
            // - Data completeness (30%)
            // - Reading accuracy (30%)
            
            Optional<KpiSnapshot> uptimeSnapshot = snapshotRepository
                    .findByMetricNameAndSnapshotDate(KPI_SENSOR_UPTIME, date);
            
            double uptimeScore = uptimeSnapshot.map(KpiSnapshot::getValue).orElse(90.0);
            double completenessScore = 95.0; // Simulated
            double accuracyScore = 98.0; // Simulated
            
            double dataQuality = (uptimeScore * 0.4) + (completenessScore * 0.3) + (accuracyScore * 0.3);
            
            saveSnapshot(KPI_DATA_QUALITY, "Data Quality Score", "Quality", 
                    date, dataQuality, 95.0, "%");
            
            log.debug("Data quality calculated: {}", dataQuality);
        } catch (Exception e) {
            log.error("Error calculating data quality: {}", e.getMessage());
        }
    }

    /**
     * Calculate corridor health index.
     */
    private void calculateCorridorHealthIndex(LocalDate date) {
        try {
            ApiResponse<Map<String, Object>> response = monitoringServiceClient.getCorridorSummary();
            Map<String, Object> summary = response.getData();
            
            double avgScore = summary.containsKey("averageHealthScore")
                    ? ((Number) summary.get("averageHealthScore")).doubleValue()
                    : 0.0;
            
            saveSnapshot(KPI_HEALTH_INDEX, "Corridor Health Index", "Health", 
                    date, avgScore, 80.0, "score");
            
            log.debug("Corridor health index calculated: {}", avgScore);
        } catch (Exception e) {
            log.error("Error calculating corridor health index: {}", e.getMessage());
        }
    }

    /**
     * Calculate average alert response time (minutes).
     */
    private void calculateAlertResponseTime(LocalDate date) {
        try {
            // This would typically query alert history to calculate average response time
            // Simulated for now
            double avgResponseTime = 15.0 + (Math.random() * 10); // 15-25 minutes
            
            saveSnapshot(KPI_ALERT_RESPONSE_TIME, "Alert Response Time", "Operations", 
                    date, avgResponseTime, 30.0, "minutes");
            
            log.debug("Alert response time calculated: {} minutes", avgResponseTime);
        } catch (Exception e) {
            log.error("Error calculating alert response time: {}", e.getMessage());
        }
    }

    /**
     * Calculate critical alerts rate.
     */
    private void calculateCriticalAlertsRate(LocalDate date) {
        try {
            ApiResponse<Map<String, Long>> response = alertServiceClient.getAlertStatistics();
            Map<String, Long> stats = response.getData();
            
            long critical = stats.getOrDefault("critical", 0L);
            long total = stats.values().stream().mapToLong(Long::longValue).sum();
            
            double rate = total > 0 ? (double) critical / total * 100.0 : 0.0;
            
            // For critical alerts, lower is better, so we track inverse for target evaluation
            saveSnapshot(KPI_CRITICAL_ALERTS, "Critical Alerts Rate", "Alerts", 
                    date, rate, 5.0, "%"); // Target: less than 5%
            
            log.debug("Critical alerts rate calculated: {}%", rate);
        } catch (Exception e) {
            log.error("Error calculating critical alerts rate: {}", e.getMessage());
        }
    }

    /**
     * Calculate asset utilization.
     */
    private void calculateAssetUtilization(LocalDate date) {
        try {
            ApiResponse<Map<String, Long>> response = assetServiceClient.getAssetCountByStatus();
            Map<String, Long> counts = response.getData();
            
            long operational = counts.getOrDefault("OPERATIONAL", 0L);
            long total = counts.values().stream().mapToLong(Long::longValue).sum();
            
            double utilization = total > 0 ? (double) operational / total * 100.0 : 0.0;
            
            saveSnapshot(KPI_ASSET_UTILIZATION, "Asset Utilization", "Assets", 
                    date, utilization, 90.0, "%");
            
            log.debug("Asset utilization calculated: {}%", utilization);
        } catch (Exception e) {
            log.error("Error calculating asset utilization: {}", e.getMessage());
        }
    }

    /**
     * Calculate construction progress (simulated).
     */
    private void calculateConstructionProgress(LocalDate date) {
        try {
            // Simulated construction progress - would come from project management system
            // Increments by 0.1-0.3% daily
            Optional<KpiSnapshot> previous = snapshotRepository
                    .findLatestByMetricName(KPI_CONSTRUCTION_PROGRESS);
            
            double previousProgress = previous.map(KpiSnapshot::getValue).orElse(45.0);
            double dailyProgress = 0.1 + (Math.random() * 0.2);
            double currentProgress = Math.min(100.0, previousProgress + dailyProgress);
            
            saveSnapshot(KPI_CONSTRUCTION_PROGRESS, "Construction Progress", "Project", 
                    date, currentProgress, 100.0, "%");
            
            log.debug("Construction progress calculated: {}%", currentProgress);
        } catch (Exception e) {
            log.error("Error calculating construction progress: {}", e.getMessage());
        }
    }

    /**
     * Calculate Schedule Performance Index.
     */
    private void calculateSchedulePerformanceIndex(LocalDate date) {
        try {
            // SPI = Earned Value / Planned Value
            // Simulated: a healthy project has SPI close to 1.0
            double spi = 0.95 + (Math.random() * 0.1); // 0.95 - 1.05
            
            saveSnapshot(KPI_SPI, "Schedule Performance Index", "Project", 
                    date, spi, 1.0, "ratio");
            
            log.debug("SPI calculated: {}", spi);
        } catch (Exception e) {
            log.error("Error calculating SPI: {}", e.getMessage());
        }
    }

    /**
     * Calculate predictive accuracy (simulated).
     */
    private void calculatePredictiveAccuracy(LocalDate date) {
        try {
            // Accuracy of health predictions vs actual outcomes
            double accuracy = 85.0 + (Math.random() * 10); // 85-95%
            
            saveSnapshot(KPI_PREDICTIVE_ACCURACY, "Predictive Model Accuracy", "Quality", 
                    date, accuracy, 90.0, "%");
            
            log.debug("Predictive accuracy calculated: {}%", accuracy);
        } catch (Exception e) {
            log.error("Error calculating predictive accuracy: {}", e.getMessage());
        }
    }

    /**
     * Calculate maintenance efficiency (simulated).
     */
    private void calculateMaintenanceEfficiency(LocalDate date) {
        try {
            // Efficiency = (Planned maintenance time / Actual maintenance time) * 100
            double efficiency = 80.0 + (Math.random() * 15); // 80-95%
            
            saveSnapshot(KPI_MAINTENANCE_EFFICIENCY, "Maintenance Efficiency", "Maintenance", 
                    date, efficiency, 85.0, "%");
            
            log.debug("Maintenance efficiency calculated: {}%", efficiency);
        } catch (Exception e) {
            log.error("Error calculating maintenance efficiency: {}", e.getMessage());
        }
    }

    /**
     * Save KPI snapshot with trend calculation.
     */
    private void saveSnapshot(String metricName, String displayName, String category,
                               LocalDate date, Double value, Double target, String unit) {
        // Get previous day's value
        Optional<KpiSnapshot> previous = snapshotRepository
                .findByMetricNameAndSnapshotDate(metricName, date.minusDays(1));
        
        // Get week ago value
        Optional<KpiSnapshot> weekAgo = snapshotRepository
                .findByMetricNameAndSnapshotDate(metricName, date.minusWeeks(1));
        
        // Get month ago value
        Optional<KpiSnapshot> monthAgo = snapshotRepository
                .findByMetricNameAndSnapshotDate(metricName, date.minusMonths(1));

        KpiSnapshot snapshot = KpiSnapshot.builder()
                .metricName(metricName)
                .displayName(displayName)
                .category(category)
                .snapshotDate(date)
                .value(value)
                .previousValue(previous.map(KpiSnapshot::getValue).orElse(null))
                .weekAgoValue(weekAgo.map(KpiSnapshot::getValue).orElse(null))
                .monthAgoValue(monthAgo.map(KpiSnapshot::getValue).orElse(null))
                .targetValue(target)
                .unit(unit)
                .build();

        snapshot.calculateTrend();
        snapshot.evaluateTarget();

        snapshotRepository.save(snapshot);
    }

    // ================= Query Methods =================

    /**
     * Get all current KPIs.
     */
    @Transactional(readOnly = true)
    @Cacheable(value = "currentKpis", key = "'all'")
    public List<KpiSnapshot> getCurrentKpis() {
        return snapshotRepository.findLatestForAllMetrics();
    }

    /**
     * Get KPIs by category.
     */
    @Transactional(readOnly = true)
    public List<KpiSnapshot> getKpisByCategory(String category) {
        return snapshotRepository.findLatestByCategory(category);
    }

    /**
     * Get KPI history.
     */
    @Transactional(readOnly = true)
    public List<KpiSnapshot> getKpiHistory(String metricName, LocalDate from, LocalDate to) {
        return snapshotRepository.findByMetricNameAndSnapshotDateBetween(metricName, from, to);
    }

    /**
     * Get dashboard summary.
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getDashboardSummary() {
        List<KpiSnapshot> currentKpis = getCurrentKpis();
        
        long onTarget = currentKpis.stream()
                .filter(k -> Boolean.TRUE.equals(k.getOnTarget()))
                .count();
        
        long improving = currentKpis.stream()
                .filter(k -> k.getTrend() == Trend.INCREASING)
                .count();
        
        long declining = currentKpis.stream()
                .filter(k -> k.getTrend() == Trend.DECREASING)
                .count();

        Map<String, Object> summary = new HashMap<>();
        summary.put("totalKpis", currentKpis.size());
        summary.put("onTargetCount", onTarget);
        summary.put("onTargetPercentage", currentKpis.isEmpty() ? 0 : (double) onTarget / currentKpis.size() * 100);
        summary.put("improvingCount", improving);
        summary.put("decliningCount", declining);
        summary.put("lastUpdated", LocalDateTime.now());
        
        return summary;
    }
}
