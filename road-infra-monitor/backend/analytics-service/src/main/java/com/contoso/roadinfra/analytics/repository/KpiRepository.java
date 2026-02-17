package com.contoso.roadinfra.analytics.repository;

import com.contoso.roadinfra.analytics.entity.Kpi;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface KpiRepository extends JpaRepository<Kpi, UUID> {

    List<Kpi> findByMetricName(String metricName);

    List<Kpi> findByCategory(String category);

    Page<Kpi> findByAssetId(UUID assetId, Pageable pageable);

    @Query("SELECT k FROM Kpi k WHERE k.metricName = :metricName ORDER BY k.calculatedAt DESC LIMIT 1")
    Optional<Kpi> findLatestByMetricName(@Param("metricName") String metricName);

    @Query("SELECT k FROM Kpi k WHERE k.assetId = :assetId AND k.metricName = :metricName ORDER BY k.calculatedAt DESC LIMIT 1")
    Optional<Kpi> findLatestByAssetAndMetric(@Param("assetId") UUID assetId, @Param("metricName") String metricName);

    List<Kpi> findByPeriodAndCalculatedAtBetween(String period, LocalDateTime start, LocalDateTime end);

    @Query("SELECT k FROM Kpi k WHERE k.onTarget = false ORDER BY k.calculatedAt DESC")
    List<Kpi> findOffTargetKpis();

    @Query("SELECT DISTINCT k.metricName FROM Kpi k")
    List<String> findAllMetricNames();

    @Query("SELECT DISTINCT k.category FROM Kpi k")
    List<String> findAllCategories();
}
