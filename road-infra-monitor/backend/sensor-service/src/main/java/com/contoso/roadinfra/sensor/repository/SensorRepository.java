package com.contoso.roadinfra.sensor.repository;

import com.contoso.roadinfra.common.constants.SensorStatus;
import com.contoso.roadinfra.common.constants.SensorType;
import com.contoso.roadinfra.sensor.entity.Sensor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SensorRepository extends JpaRepository<Sensor, UUID> {

    Optional<Sensor> findBySensorCode(String sensorCode);

    boolean existsBySensorCode(String sensorCode);

    List<Sensor> findByAssetId(UUID assetId);

    Page<Sensor> findByAssetId(UUID assetId, Pageable pageable);

    List<Sensor> findBySensorType(SensorType sensorType);

    List<Sensor> findByStatus(SensorStatus status);

    Page<Sensor> findByStatus(SensorStatus status, Pageable pageable);

    @Query("SELECT s FROM Sensor s WHERE s.status IN :statuses")
    List<Sensor> findByStatusIn(@Param("statuses") List<SensorStatus> statuses);

    @Query("SELECT s FROM Sensor s WHERE s.status NOT IN :excludedStatuses")
    List<Sensor> findByStatusNotIn(@Param("excludedStatuses") List<SensorStatus> excludedStatuses);

    @Query("SELECT s FROM Sensor s WHERE s.batteryLevel < :threshold AND s.status = 'ACTIVE'")
    List<Sensor> findLowBatterySensors(@Param("threshold") Double threshold);

    @Query("SELECT COUNT(s) FROM Sensor s WHERE s.assetId = :assetId")
    long countByAssetId(@Param("assetId") UUID assetId);

    @Query("SELECT COUNT(s) FROM Sensor s WHERE s.status = :status")
    long countByStatus(@Param("status") SensorStatus status);

    @Query("SELECT s FROM Sensor s WHERE s.latitude BETWEEN :minLat AND :maxLat " +
            "AND s.longitude BETWEEN :minLon AND :maxLon")
    List<Sensor> findInBoundingBox(@Param("minLat") Double minLat,
                                    @Param("maxLat") Double maxLat,
                                    @Param("minLon") Double minLon,
                                    @Param("maxLon") Double maxLon);

    // Aggregation queries for statistics
    @Query("SELECT s.sensorType, COUNT(s) FROM Sensor s GROUP BY s.sensorType")
    List<Object[]> countGroupedByType();

    @Query("SELECT s.status, COUNT(s) FROM Sensor s GROUP BY s.status")
    List<Object[]> countGroupedByStatus();

    // Find sensors that haven't reported data recently (for offline detection)
    @Query("SELECT s FROM Sensor s WHERE s.status = 'ACTIVE' AND " +
           "(s.lastDataReceivedAt IS NULL OR s.lastDataReceivedAt < :threshold)")
    List<Sensor> findSensorsWithNoRecentData(@Param("threshold") Instant threshold);

    // Find sensors due for calibration
    @Query("SELECT s FROM Sensor s WHERE s.calibrationIntervalDays IS NOT NULL AND " +
           "s.lastCalibrationDate IS NOT NULL AND s.status != 'DECOMMISSIONED' AND " +
           "FUNCTION('DATE_ADD', s.lastCalibrationDate, s.calibrationIntervalDays, 'DAY') <= :checkDate")
    List<Sensor> findSensorsDueForCalibration(@Param("checkDate") LocalDate checkDate);

    // Alternative native query for calibration check (more reliable across dialects)
    @Query(value = "SELECT * FROM sensors s WHERE s.calibration_interval_days IS NOT NULL AND " +
           "s.last_calibration_date IS NOT NULL AND s.status != 'DECOMMISSIONED' AND " +
           "(s.last_calibration_date + s.calibration_interval_days * INTERVAL '1 day') <= :checkDate",
           nativeQuery = true)
    List<Sensor> findSensorsDueForCalibrationNative(@Param("checkDate") LocalDate checkDate);

    // Update sensor status in bulk
    @Modifying
    @Query("UPDATE Sensor s SET s.status = :newStatus, s.updatedAt = :now, s.updatedBy = :updatedBy " +
           "WHERE s.id IN :sensorIds")
    int updateStatusForSensors(@Param("sensorIds") List<UUID> sensorIds,
                                @Param("newStatus") SensorStatus newStatus,
                                @Param("now") Instant now,
                                @Param("updatedBy") String updatedBy);
}
