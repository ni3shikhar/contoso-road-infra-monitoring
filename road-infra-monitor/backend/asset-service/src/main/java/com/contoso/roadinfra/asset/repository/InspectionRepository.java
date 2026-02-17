package com.contoso.roadinfra.asset.repository;

import com.contoso.roadinfra.asset.constants.InspectionType;
import com.contoso.roadinfra.asset.entity.AssetInspection;
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
public interface InspectionRepository extends JpaRepository<AssetInspection, UUID> {

    // Find inspections for a specific asset
    List<AssetInspection> findByAssetIdOrderByInspectionDateDesc(UUID assetId);

    Page<AssetInspection> findByAssetIdOrderByInspectionDateDesc(UUID assetId, Pageable pageable);

    // Find latest inspection for an asset
    Optional<AssetInspection> findFirstByAssetIdOrderByInspectionDateDesc(UUID assetId);

    // Find inspections by type
    List<AssetInspection> findByInspectionType(InspectionType inspectionType);

    // Find inspections in date range
    List<AssetInspection> findByInspectionDateBetween(LocalDate startDate, LocalDate endDate);

    // Find inspections with critical conditions (rating <= 2)
    @Query("SELECT i FROM AssetInspection i WHERE i.overallConditionRating <= 2 ORDER BY i.inspectionDate DESC")
    List<AssetInspection> findCriticalInspections();

    // Find inspections by rating
    List<AssetInspection> findByOverallConditionRatingLessThanEqual(Integer rating);

    // Count inspections for an asset
    long countByAssetId(UUID assetId);

    // Get average rating for an asset
    @Query("SELECT AVG(i.overallConditionRating) FROM AssetInspection i WHERE i.asset.id = :assetId")
    Double getAverageRatingForAsset(@Param("assetId") UUID assetId);

    // Find assets with overdue inspections (join query)
    @Query("SELECT DISTINCT i.asset.id FROM AssetInspection i WHERE i.nextInspectionRecommendedDate <= :date")
    List<UUID> findAssetIdsWithOverdueInspections(@Param("date") LocalDate date);

    // Find recent inspections across all assets
    @Query("SELECT i FROM AssetInspection i ORDER BY i.inspectionDate DESC")
    Page<AssetInspection> findRecentInspections(Pageable pageable);

    // Find recent low-rated inspections for follow-up scheduling
    List<AssetInspection> findByInspectionDateAfterAndOverallConditionRatingLessThanEqual(
            LocalDate date, Integer rating);
}
