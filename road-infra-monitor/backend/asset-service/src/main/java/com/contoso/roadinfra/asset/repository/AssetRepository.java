package com.contoso.roadinfra.asset.repository;

import com.contoso.roadinfra.asset.constants.ConstructionStatus;
import com.contoso.roadinfra.asset.entity.Asset;
import com.contoso.roadinfra.common.constants.AssetType;
import com.contoso.roadinfra.common.constants.HealthStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AssetRepository extends JpaRepository<Asset, UUID> {

    Optional<Asset> findByAssetCode(String assetCode);

    boolean existsByAssetCode(String assetCode);

    List<Asset> findByAssetType(AssetType assetType);

    Page<Asset> findByAssetType(AssetType assetType, Pageable pageable);

    List<Asset> findByHealthStatus(HealthStatus status);

    List<Asset> findByStatus(ConstructionStatus status);

    Page<Asset> findByStatus(ConstructionStatus status, Pageable pageable);

    // Find child assets by parent ID
    List<Asset> findByParentAssetId(UUID parentAssetId);

    // Find root assets (no parent)
    List<Asset> findByParentAssetIdIsNull();

    Page<Asset> findByParentAssetIdIsNull(Pageable pageable);

    // Count children for a parent asset
    long countByParentAssetId(UUID parentAssetId);

    // Find assets with overdue inspections
    @Query("SELECT a FROM Asset a WHERE a.nextInspectionDate <= :date")
    List<Asset> findAssetsWithOverdueInspection(@Param("date") LocalDate date);

    // Find assets by health status
    @Query("SELECT a FROM Asset a WHERE a.healthStatus IN :statuses")
    List<Asset> findByHealthStatusIn(@Param("statuses") List<HealthStatus> statuses);

    // Find assets within chainage range
    @Query("SELECT a FROM Asset a WHERE a.startChainage >= :start AND a.endChainage <= :end ORDER BY a.startChainage")
    List<Asset> findByChainageRange(@Param("start") Double start, @Param("end") Double end);

    // Find assets in bounding box
    @Query("SELECT a FROM Asset a WHERE a.startLatitude BETWEEN :minLat AND :maxLat " +
            "AND a.startLongitude BETWEEN :minLon AND :maxLon")
    List<Asset> findInBoundingBox(@Param("minLat") Double minLat,
                                   @Param("maxLat") Double maxLat,
                                   @Param("minLon") Double minLon,
                                   @Param("maxLon") Double maxLon);

    // Statistics queries
    @Query("SELECT COUNT(a) FROM Asset a WHERE a.assetType = :type")
    long countByAssetType(@Param("type") AssetType type);

    @Query("SELECT COUNT(a) FROM Asset a WHERE a.healthStatus = :status")
    long countByHealthStatus(@Param("status") HealthStatus status);

    // Find critical or warning assets
    @Query("SELECT a FROM Asset a WHERE a.healthStatus IN ('CRITICAL', 'WARNING')")
    List<Asset> findCriticalAndWarningAssets();

    // Get average completion percentage
    @Query("SELECT AVG(a.completionPercentage) FROM Asset a")
    Double getAverageCompletionPercentage();

    // Get total corridor length
    @Query("SELECT SUM(a.length) FROM Asset a WHERE a.parentAssetId IS NULL")
    Double getTotalCorridorLength();

    // Group by queries for statistics
    @Query("SELECT a.assetType, COUNT(a) FROM Asset a GROUP BY a.assetType")
    List<Object[]> countGroupedByType();

    @Query("SELECT a.healthStatus, COUNT(a) FROM Asset a GROUP BY a.healthStatus")
    List<Object[]> countGroupedByHealthStatus();

    @Query("SELECT a.status, COUNT(a) FROM Asset a GROUP BY a.status")
    List<Object[]> countGroupedByConstructionStatus();
}
