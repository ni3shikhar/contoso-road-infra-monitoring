package com.contoso.roadinfra.monitoring.repository;

import com.contoso.roadinfra.common.constants.HealthStatus;
import com.contoso.roadinfra.monitoring.entity.HealthRecord;
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
public interface HealthRecordRepository extends JpaRepository<HealthRecord, UUID> {

    Page<HealthRecord> findByAssetId(UUID assetId, Pageable pageable);

    Page<HealthRecord> findBySensorId(UUID sensorId, Pageable pageable);

    List<HealthRecord> findByStatus(HealthStatus status);

    @Query("SELECT h FROM HealthRecord h WHERE h.assetId = :assetId ORDER BY h.checkedAt DESC LIMIT 1")
    Optional<HealthRecord> findLatestByAssetId(@Param("assetId") UUID assetId);

    @Query("SELECT h FROM HealthRecord h WHERE h.sensorId = :sensorId ORDER BY h.checkedAt DESC LIMIT 1")
    Optional<HealthRecord> findLatestBySensorId(@Param("sensorId") UUID sensorId);

    List<HealthRecord> findByAssetIdAndCheckedAtBetween(UUID assetId, LocalDateTime start, LocalDateTime end);

    @Query("SELECT h FROM HealthRecord h WHERE h.statusChanged = true AND h.checkedAt >= :since")
    List<HealthRecord> findStatusChanges(@Param("since") LocalDateTime since);

    @Query("SELECT AVG(h.healthScore) FROM HealthRecord h WHERE h.assetId = :assetId AND h.checkedAt >= :since")
    Double calculateAverageHealthScore(@Param("assetId") UUID assetId, @Param("since") LocalDateTime since);
}
