package com.contoso.roadinfra.analytics.service;

import com.contoso.roadinfra.analytics.entity.Kpi;
import com.contoso.roadinfra.analytics.repository.KpiRepository;
import com.contoso.roadinfra.common.dto.KpiDTO;
import com.contoso.roadinfra.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
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
public class AnalyticsService {

    private final KpiRepository kpiRepository;

    @Transactional(readOnly = true)
    public KpiDTO getKpiById(UUID id) {
        Kpi kpi = kpiRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("KPI", id));
        return toDto(kpi);
    }

    @Cacheable(value = "kpis", key = "#metricName")
    @Transactional(readOnly = true)
    public KpiDTO getLatestKpi(String metricName) {
        Kpi kpi = kpiRepository.findLatestByMetricName(metricName)
                .orElseThrow(() -> new ResourceNotFoundException("KPI", metricName));
        return toDto(kpi);
    }

    @Transactional(readOnly = true)
    public List<KpiDTO> getKpisByCategory(String category) {
        return kpiRepository.findByCategory(category).stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public Page<KpiDTO> getKpisByAsset(UUID assetId, Pageable pageable) {
        return kpiRepository.findByAssetId(assetId, pageable).map(this::toDto);
    }

    @Transactional(readOnly = true)
    public List<KpiDTO> getOffTargetKpis() {
        return kpiRepository.findOffTargetKpis().stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<String> getAllMetricNames() {
        return kpiRepository.findAllMetricNames();
    }

    @Transactional(readOnly = true)
    public List<String> getAllCategories() {
        return kpiRepository.findAllCategories();
    }

    public KpiDTO calculateKpi(String metricName, String category, UUID assetId, String assetName, Double value, Double targetValue, String unit, String period) {
        log.info("Calculating KPI: {} for asset: {}", metricName, assetId);

        Kpi previousKpi = kpiRepository.findLatestByAssetAndMetric(assetId, metricName).orElse(null);
        Double previousValue = previousKpi != null ? previousKpi.getValue() : null;
        Double percentageChange = calculatePercentageChange(previousValue, value);
        String trend = calculateTrend(previousValue, value);
        Boolean onTarget = targetValue != null && value >= targetValue;

        Kpi kpi = Kpi.builder()
                .metricName(metricName)
                .displayName(formatDisplayName(metricName))
                .category(category)
                .value(value)
                .previousValue(previousValue)
                .targetValue(targetValue)
                .unit(unit)
                .percentageChange(percentageChange)
                .trend(trend)
                .onTarget(onTarget)
                .assetId(assetId)
                .assetName(assetName)
                .period(period)
                .periodStart(LocalDateTime.now().minusDays(1))
                .periodEnd(LocalDateTime.now())
                .calculatedAt(LocalDateTime.now())
                .build();

        Kpi saved = kpiRepository.save(kpi);
        return toDto(saved);
    }

    @Scheduled(cron = "0 0 * * * *") // Every hour
    public void calculateSystemKpis() {
        log.info("Running scheduled KPI calculation");
        // Calculate system-wide KPIs
    }

    private Double calculatePercentageChange(Double previous, Double current) {
        if (previous == null || previous == 0) return null;
        return ((current - previous) / previous) * 100;
    }

    private String calculateTrend(Double previous, Double current) {
        if (previous == null) return "STABLE";
        double diff = current - previous;
        if (diff > 0) return "UP";
        if (diff < 0) return "DOWN";
        return "STABLE";
    }

    private String formatDisplayName(String metricName) {
        return metricName.replace("_", " ")
                .toLowerCase()
                .replaceFirst("^.", String.valueOf(Character.toUpperCase(metricName.charAt(0))));
    }

    private KpiDTO toDto(Kpi kpi) {
        return KpiDTO.builder()
                .id(kpi.getId())
                .metricName(kpi.getMetricName())
                .displayName(kpi.getDisplayName())
                .category(kpi.getCategory())
                .value(kpi.getValue())
                .previousValue(kpi.getPreviousValue())
                .targetValue(kpi.getTargetValue())
                .unit(kpi.getUnit())
                .percentageChange(kpi.getPercentageChange())
                .trend(kpi.getTrend())
                .onTarget(kpi.getOnTarget())
                .assetId(kpi.getAssetId())
                .assetName(kpi.getAssetName())
                .period(kpi.getPeriod())
                .periodStart(kpi.getPeriodStart())
                .periodEnd(kpi.getPeriodEnd())
                .historicalData(kpi.getHistoricalData())
                .breakdown(kpi.getBreakdown())
                .calculatedAt(kpi.getCalculatedAt())
                .createdAt(kpi.getCreatedAt())
                .build();
    }
}
