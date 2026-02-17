package com.contoso.roadinfra.asset.repository;

import com.contoso.roadinfra.asset.constants.MilestoneStatus;
import com.contoso.roadinfra.asset.entity.ConstructionMilestone;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface MilestoneRepository extends JpaRepository<ConstructionMilestone, UUID> {

    // Find milestones for a specific asset ordered by sequence
    List<ConstructionMilestone> findByAssetIdOrderBySequenceOrder(UUID assetId);

    Page<ConstructionMilestone> findByAssetIdOrderBySequenceOrder(UUID assetId, Pageable pageable);

    // Find milestones by status
    List<ConstructionMilestone> findByStatus(MilestoneStatus status);

    Page<ConstructionMilestone> findByStatus(MilestoneStatus status, Pageable pageable);

    // Find milestones by status for a specific asset
    List<ConstructionMilestone> findByAssetIdAndStatus(UUID assetId, MilestoneStatus status);

    // Find delayed milestones (past due and not completed)
    @Query("SELECT m FROM ConstructionMilestone m WHERE m.plannedDate < :today " +
           "AND m.status NOT IN ('COMPLETED', 'CANCELLED') ORDER BY m.plannedDate")
    List<ConstructionMilestone> findDelayedMilestones(@Param("today") LocalDate today);

    // Find delayed milestones with pagination
    @Query("SELECT m FROM ConstructionMilestone m WHERE m.plannedDate < :today " +
           "AND m.status NOT IN ('COMPLETED', 'CANCELLED') ORDER BY m.plannedDate")
    Page<ConstructionMilestone> findDelayedMilestones(@Param("today") LocalDate today, Pageable pageable);

    // Find upcoming milestones within days
    @Query("SELECT m FROM ConstructionMilestone m WHERE m.plannedDate BETWEEN :start AND :end " +
           "AND m.status NOT IN ('COMPLETED', 'CANCELLED') ORDER BY m.plannedDate")
    List<ConstructionMilestone> findUpcomingMilestones(@Param("start") LocalDate start, @Param("end") LocalDate end);

    // Count milestones by status for an asset
    long countByAssetIdAndStatus(UUID assetId, MilestoneStatus status);

    // Count all milestones for an asset
    long countByAssetId(UUID assetId);

    // Count completed milestones for an asset
    @Query("SELECT COUNT(m) FROM ConstructionMilestone m WHERE m.asset.id = :assetId AND m.status = 'COMPLETED'")
    long countCompletedByAssetId(@Param("assetId") UUID assetId);

    // Get milestones with significant delays - find past-due milestones where planned date is more than N days ago
    @Query("SELECT m FROM ConstructionMilestone m WHERE m.plannedDate < :cutoffDate " +
           "AND m.status NOT IN ('COMPLETED', 'CANCELLED') ORDER BY m.plannedDate ASC")
    List<ConstructionMilestone> findMilestonesWithDelayGreaterThan(@Param("cutoffDate") LocalDate cutoffDate);

    // Find next milestone for an asset
    @Query("SELECT m FROM ConstructionMilestone m WHERE m.asset.id = :assetId " +
           "AND m.status NOT IN ('COMPLETED', 'CANCELLED') ORDER BY m.sequenceOrder ASC")
    List<ConstructionMilestone> findNextMilestonesForAsset(@Param("assetId") UUID assetId);

    // Get average delay for an asset (calculated as difference between actual and planned dates)
    @Query(value = "SELECT AVG(EXTRACT(DAY FROM (m.actual_completion_date - m.planned_date))) FROM construction_milestones m " +
           "WHERE m.asset_id = :assetId AND m.status = 'COMPLETED' AND m.actual_completion_date IS NOT NULL", nativeQuery = true)
    Double getAverageDelayForAsset(@Param("assetId") UUID assetId);
}
