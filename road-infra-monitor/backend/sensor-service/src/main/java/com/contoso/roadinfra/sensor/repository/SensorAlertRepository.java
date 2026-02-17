package com.contoso.roadinfra.sensor.repository;

import com.contoso.roadinfra.common.constants.AlertSeverity;
import com.contoso.roadinfra.common.constants.SensorAlertType;
import com.contoso.roadinfra.sensor.entity.SensorAlert;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface SensorAlertRepository extends JpaRepository<SensorAlert, UUID> {

    Page<SensorAlert> findAllByOrderByCreatedAtDesc(Pageable pageable);

    Page<SensorAlert> findBySensorIdOrderByCreatedAtDesc(UUID sensorId, Pageable pageable);

    List<SensorAlert> findBySensorIdAndAcknowledgedFalseOrderByCreatedAtDesc(UUID sensorId);

    Page<SensorAlert> findByAcknowledgedFalseOrderByCreatedAtDesc(Pageable pageable);

    Page<SensorAlert> findBySeverityOrderByCreatedAtDesc(AlertSeverity severity, Pageable pageable);

    Page<SensorAlert> findByAlertTypeOrderByCreatedAtDesc(SensorAlertType alertType, Pageable pageable);

    @Query("SELECT sa FROM SensorAlert sa WHERE sa.acknowledged = false AND " +
           "sa.createdAt BETWEEN :start AND :end ORDER BY sa.createdAt DESC")
    List<SensorAlert> findUnacknowledgedInPeriod(@Param("start") Instant start,
                                                  @Param("end") Instant end);

    @Query("SELECT COUNT(sa) FROM SensorAlert sa WHERE sa.sensorId = :sensorId AND sa.acknowledged = false")
    Long countUnacknowledgedBySensor(@Param("sensorId") UUID sensorId);

    @Query("SELECT COUNT(sa) FROM SensorAlert sa WHERE sa.acknowledged = false")
    Long countAllUnacknowledged();

    @Query("SELECT sa.alertType, COUNT(sa) FROM SensorAlert sa WHERE sa.acknowledged = false GROUP BY sa.alertType")
    List<Object[]> countUnacknowledgedGroupedByType();

    @Query("SELECT sa.severity, COUNT(sa) FROM SensorAlert sa WHERE sa.acknowledged = false GROUP BY sa.severity")
    List<Object[]> countUnacknowledgedGroupedBySeverity();

    // Check if there's a recent unacknowledged alert of the same type for a sensor
    @Query("SELECT COUNT(sa) > 0 FROM SensorAlert sa WHERE sa.sensorId = :sensorId " +
           "AND sa.alertType = :alertType AND sa.acknowledged = false AND sa.createdAt > :since")
    boolean existsRecentUnacknowledgedAlert(@Param("sensorId") UUID sensorId,
                                            @Param("alertType") SensorAlertType alertType,
                                            @Param("since") Instant since);

    // Bulk acknowledge alerts
    @Modifying
    @Query("UPDATE SensorAlert sa SET sa.acknowledged = true, sa.acknowledgedBy = :username, " +
           "sa.acknowledgedAt = :now WHERE sa.id IN :alertIds AND sa.acknowledged = false")
    int acknowledgeAlerts(@Param("alertIds") List<UUID> alertIds,
                          @Param("username") String username,
                          @Param("now") Instant now);

    // Delete old acknowledged alerts for data retention
    @Modifying
    @Query("DELETE FROM SensorAlert sa WHERE sa.acknowledged = true AND sa.createdAt < :before")
    int deleteAcknowledgedAlertsBefore(@Param("before") Instant before);
}
