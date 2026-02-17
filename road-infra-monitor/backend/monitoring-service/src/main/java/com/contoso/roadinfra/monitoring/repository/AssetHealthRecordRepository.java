package com.contoso.roadinfra.monitoring.repository;

import com.contoso.roadinfra.common.constants.AssetType;
import com.contoso.roadinfra.common.constants.HealthStatus;
import com.contoso.roadinfra.monitoring.entity.AssetHealthRecord;
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
public interface AssetHealthRecordRepository extends JpaRepository<AssetHealthRecord, UUID> {

    // Find latest health record for an asset
    @Query("SELECT r FROM AssetHealthRecord r WHERE r.assetId = :assetId ORDER BY r.timestamp DESC LIMIT 1")
    Optional<AssetHealthRecord> findLatestByAssetId(@Param("assetId") UUID assetId);

    // Find all latest health records (one per asset)
    @Query("""
        SELECT r FROM AssetHealthRecord r 
        WHERE r.timestamp = (
            SELECT MAX(r2.timestamp) FROM AssetHealthRecord r2 WHERE r2.assetId = r.assetId
        )
        ORDER BY r.healthStatus DESC, r.overallHealthScore ASC
        """)
    List<AssetHealthRecord> findAllLatest();

    // Find latest health records with pagination
    @Query("""
        SELECT r FROM AssetHealthRecord r 
        WHERE r.timestamp = (
            SELECT MAX(r2.timestamp) FROM AssetHealthRecord r2 WHERE r2.assetId = r.assetId
        )
        """)
    Page<AssetHealthRecord> findAllLatest(Pageable pageable);

    // Find health history for an asset
    Page<AssetHealthRecord> findByAssetIdOrderByTimestampDesc(UUID assetId, Pageable pageable);

    // Find health history within time range
    List<AssetHealthRecord> findByAssetIdAndTimestampBetweenOrderByTimestampDesc(
            UUID assetId, LocalDateTime start, LocalDateTime end);

    // Count assets by health status (latest records only)
    @Query("""
        SELECT r.healthStatus, COUNT(DISTINCT r.assetId) FROM AssetHealthRecord r 
        WHERE r.timestamp = (
            SELECT MAX(r2.timestamp) FROM AssetHealthRecord r2 WHERE r2.assetId = r.assetId
        )
        GROUP BY r.healthStatus
        """)
    List<Object[]> countAssetsByHealthStatus();

    // Find assets in specific health status
    @Query("""
        SELECT r FROM AssetHealthRecord r 
        WHERE r.healthStatus = :status 
        AND r.timestamp = (
            SELECT MAX(r2.timestamp) FROM AssetHealthRecord r2 WHERE r2.assetId = r.assetId
        )
        """)
    List<AssetHealthRecord> findByHealthStatus(@Param("status") HealthStatus status);

    // Find assets with health score below threshold
    @Query("""
        SELECT r FROM AssetHealthRecord r 
        WHERE r.overallHealthScore < :threshold 
        AND r.timestamp = (
            SELECT MAX(r2.timestamp) FROM AssetHealthRecord r2 WHERE r2.assetId = r.assetId
        )
        ORDER BY r.overallHealthScore ASC
        """)
    List<AssetHealthRecord> findByHealthScoreBelow(@Param("threshold") Double threshold);

    // Find assets by type with latest health
    @Query("""
        SELECT r FROM AssetHealthRecord r 
        WHERE r.assetType = :assetType 
        AND r.timestamp = (
            SELECT MAX(r2.timestamp) FROM AssetHealthRecord r2 WHERE r2.assetId = r.assetId
        )
        """)
    List<AssetHealthRecord> findLatestByAssetType(@Param("assetType") AssetType assetType);

    // Get average health score by asset type
    @Query("""
        SELECT r.assetType, AVG(r.overallHealthScore) FROM AssetHealthRecord r 
        WHERE r.timestamp = (
            SELECT MAX(r2.timestamp) FROM AssetHealthRecord r2 WHERE r2.assetId = r.assetId
        )
        GROUP BY r.assetType
        """)
    List<Object[]> getAverageScoreByAssetType();

    // Get total sensor counts across all latest records
    @Query("""
        SELECT SUM(r.activeSensorCount), SUM(r.totalSensorCount), SUM(r.faultySensorCount), SUM(r.activeAlertCount)
        FROM AssetHealthRecord r 
        WHERE r.timestamp = (
            SELECT MAX(r2.timestamp) FROM AssetHealthRecord r2 WHERE r2.assetId = r.assetId
        )
        """)
    Object[] getTotalSensorCounts();

    // Get overall average health score
    @Query("""
        SELECT AVG(r.overallHealthScore) FROM AssetHealthRecord r 
        WHERE r.timestamp = (
            SELECT MAX(r2.timestamp) FROM AssetHealthRecord r2 WHERE r2.assetId = r.assetId
        )
        """)
    Double getOverallAverageScore();

    // Check if asset has health record
    boolean existsByAssetId(UUID assetId);

    // Delete old records (for cleanup)
    void deleteByTimestampBefore(LocalDateTime cutoff);

    // Find all latest records as a list (alias for service compatibility)
    default List<AssetHealthRecord> findAllLatestRecords() {
        return findAllLatest();
    }

    // Find latest by asset type with pagination
    @Query("""
        SELECT r FROM AssetHealthRecord r 
        WHERE r.assetType = :assetType 
        AND r.timestamp = (
            SELECT MAX(r2.timestamp) FROM AssetHealthRecord r2 WHERE r2.assetId = r.assetId
        )
        """)
    Page<AssetHealthRecord> findLatestByAssetType(@Param("assetType") AssetType assetType, Pageable pageable);

    // Find latest by health status with pagination
    @Query("""
        SELECT r FROM AssetHealthRecord r 
        WHERE r.healthStatus = :status 
        AND r.timestamp = (
            SELECT MAX(r2.timestamp) FROM AssetHealthRecord r2 WHERE r2.assetId = r.assetId
        )
        """)
    Page<AssetHealthRecord> findLatestByStatus(@Param("status") HealthStatus status, Pageable pageable);

    // Find latest by health status as list
    @Query("""
        SELECT r FROM AssetHealthRecord r 
        WHERE r.healthStatus = :status 
        AND r.timestamp = (
            SELECT MAX(r2.timestamp) FROM AssetHealthRecord r2 WHERE r2.assetId = r.assetId
        )
        """)
    List<AssetHealthRecord> findLatestByStatus(@Param("status") HealthStatus status);

    // Find latest by asset type and health status with pagination
    @Query("""
        SELECT r FROM AssetHealthRecord r 
        WHERE r.assetType = :assetType AND r.healthStatus = :status
        AND r.timestamp = (
            SELECT MAX(r2.timestamp) FROM AssetHealthRecord r2 WHERE r2.assetId = r.assetId
        )
        """)
    Page<AssetHealthRecord> findLatestByAssetTypeAndStatus(
            @Param("assetType") AssetType assetType,
            @Param("status") HealthStatus status, 
            Pageable pageable);

    // Find records by asset ID and timestamp range for history
    Page<AssetHealthRecord> findByAssetIdAndTimestampBetween(
            UUID assetId, LocalDateTime from, LocalDateTime to, Pageable pageable);

    // Find recent records for trend calculation
    List<AssetHealthRecord> findTop5ByAssetIdOrderByTimestampDesc(UUID assetId);

    // Find by asset type and enabled thresholds
    @Query("SELECT r FROM AssetHealthRecord r WHERE r.assetType = :assetType")
    List<AssetHealthRecord> findByAssetType(@Param("assetType") AssetType assetType);
}
