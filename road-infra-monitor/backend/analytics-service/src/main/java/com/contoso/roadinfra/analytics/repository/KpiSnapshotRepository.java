package com.contoso.roadinfra.analytics.repository;

import com.contoso.roadinfra.analytics.constants.Trend;
import com.contoso.roadinfra.analytics.entity.KpiSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for KpiSnapshot entities.
 */
@Repository
public interface KpiSnapshotRepository extends JpaRepository<KpiSnapshot, UUID> {

    /**
     * Find snapshot by metric name and date.
     */
    Optional<KpiSnapshot> findByMetricNameAndSnapshotDate(String metricName, LocalDate snapshotDate);

    /**
     * Find latest snapshot for a metric.
     */
    @Query("SELECT k FROM KpiSnapshot k WHERE k.metricName = :metricName ORDER BY k.snapshotDate DESC LIMIT 1")
    Optional<KpiSnapshot> findLatestByMetricName(@Param("metricName") String metricName);

    /**
     * Find history for a metric within date range.
     */
    List<KpiSnapshot> findByMetricNameAndSnapshotDateBetween(
            String metricName, LocalDate from, LocalDate to);

    /**
     * Find history for a metric, ordered by date.
     */
    List<KpiSnapshot> findByMetricNameOrderBySnapshotDateDesc(String metricName);

    /**
     * Find latest snapshot for each metric.
     */
    @Query("""
        SELECT k FROM KpiSnapshot k 
        WHERE k.snapshotDate = (
            SELECT MAX(k2.snapshotDate) FROM KpiSnapshot k2 WHERE k2.metricName = k.metricName
        )
        ORDER BY k.category, k.metricName
        """)
    List<KpiSnapshot> findLatestForAllMetrics();

    /**
     * Find latest snapshots by category.
     */
    @Query("""
        SELECT k FROM KpiSnapshot k 
        WHERE k.category = :category 
        AND k.snapshotDate = (
            SELECT MAX(k2.snapshotDate) FROM KpiSnapshot k2 
            WHERE k2.metricName = k.metricName
        )
        ORDER BY k.metricName
        """)
    List<KpiSnapshot> findLatestByCategory(@Param("category") String category);

    /**
     * Find snapshots not meeting target.
     */
    @Query("""
        SELECT k FROM KpiSnapshot k 
        WHERE k.onTarget = false 
        AND k.snapshotDate = (
            SELECT MAX(k2.snapshotDate) FROM KpiSnapshot k2 WHERE k2.metricName = k.metricName
        )
        ORDER BY k.category, k.metricName
        """)
    List<KpiSnapshot> findLatestOffTarget();

    /**
     * Find snapshots with specific trend.
     */
    @Query("""
        SELECT k FROM KpiSnapshot k 
        WHERE k.trend = :trend 
        AND k.snapshotDate = (
            SELECT MAX(k2.snapshotDate) FROM KpiSnapshot k2 WHERE k2.metricName = k.metricName
        )
        """)
    List<KpiSnapshot> findLatestByTrend(@Param("trend") Trend trend);

    /**
     * Get all distinct categories.
     */
    @Query("SELECT DISTINCT k.category FROM KpiSnapshot k ORDER BY k.category")
    List<String> findAllCategories();

    /**
     * Get all distinct metric names.
     */
    @Query("SELECT DISTINCT k.metricName FROM KpiSnapshot k ORDER BY k.metricName")
    List<String> findAllMetricNames();

    /**
     * Count snapshots by category.
     */
    @Query("""
        SELECT k.category, COUNT(DISTINCT k.metricName) FROM KpiSnapshot k 
        GROUP BY k.category
        """)
    List<Object[]> countMetricsByCategory();

    /**
     * Delete old snapshots (for cleanup).
     */
    void deleteBySnapshotDateBefore(LocalDate cutoff);
}
