package com.contoso.roadinfra.alert.repository;

import com.contoso.roadinfra.alert.constants.AlertStatus;
import com.contoso.roadinfra.alert.entity.Alert;
import com.contoso.roadinfra.common.constants.AlertSeverity;
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
public interface AlertRepository extends JpaRepository<Alert, UUID> {

    Page<Alert> findByAssetId(UUID assetId, Pageable pageable);

    Page<Alert> findBySensorId(UUID sensorId, Pageable pageable);

    List<Alert> findBySeverity(AlertSeverity severity);

    List<Alert> findByStatus(String status);

    @Query("SELECT a FROM Alert a WHERE a.resolved = false ORDER BY a.severity DESC, a.triggeredAt DESC")
    List<Alert> findActiveAlerts();

    @Query("SELECT a FROM Alert a WHERE a.acknowledged = false AND a.severity IN :severities")
    List<Alert> findUnacknowledgedAlerts(@Param("severities") List<AlertSeverity> severities);

    @Query("SELECT a FROM Alert a WHERE a.assetId = :assetId AND a.resolved = false")
    List<Alert> findActiveAlertsByAsset(@Param("assetId") UUID assetId);

    @Query("SELECT COUNT(a) FROM Alert a WHERE a.assetId = :assetId AND a.resolved = false")
    long countActiveAlertsByAsset(@Param("assetId") UUID assetId);

    @Query("SELECT COUNT(a) FROM Alert a WHERE a.severity = :severity AND a.resolved = false")
    long countActiveBySeverity(@Param("severity") AlertSeverity severity);

    List<Alert> findByTriggeredAtBetween(LocalDateTime start, LocalDateTime end);

    @Query("SELECT a FROM Alert a WHERE a.notificationsSent = false AND a.resolved = false")
    List<Alert> findPendingNotifications();

    // Find alerts by alert status enum
    List<Alert> findByAlertStatus(AlertStatus alertStatus);

    Page<Alert> findByAlertStatus(AlertStatus alertStatus, Pageable pageable);

    // Find alerts needing escalation (open longer than configured time)
    @Query("""
        SELECT a FROM Alert a 
        WHERE a.alertStatus IN (com.contoso.roadinfra.alert.constants.AlertStatus.OPEN, 
                                com.contoso.roadinfra.alert.constants.AlertStatus.ACKNOWLEDGED)
        AND a.triggeredAt < :cutoff
        ORDER BY a.severity DESC, a.triggeredAt ASC
        """)
    List<Alert> findAlertsNeedingEscalation(@Param("cutoff") LocalDateTime cutoff);

    // Find recent alert by rule and asset (for cooldown check)
    @Query("""
        SELECT a FROM Alert a 
        WHERE a.ruleId = :ruleId 
        AND a.assetId = :assetId 
        AND a.triggeredAt > :since
        ORDER BY a.triggeredAt DESC
        LIMIT 1
        """)
    Optional<Alert> findRecentByRuleAndAsset(
            @Param("ruleId") UUID ruleId,
            @Param("assetId") UUID assetId,
            @Param("since") LocalDateTime since);

    // Count active alerts by asset type
    @Query("""
        SELECT COUNT(a) FROM Alert a 
        WHERE a.resolved = false 
        AND a.assetId IN (SELECT DISTINCT r.assetId FROM Alert r WHERE r.category = :category)
        """)
    long countActiveByCategory(@Param("category") String category);

    // Find all active alerts with pagination and filtering
    @Query("""
        SELECT a FROM Alert a 
        WHERE a.resolved = false
        AND (:severity IS NULL OR a.severity = :severity)
        AND (:assetId IS NULL OR a.assetId = :assetId)
        ORDER BY a.severity DESC, a.triggeredAt DESC
        """)
    Page<Alert> findActiveAlerts(
            @Param("severity") AlertSeverity severity,
            @Param("assetId") UUID assetId,
            Pageable pageable);

    // Count by severity for dashboard
    @Query("""
        SELECT a.severity, COUNT(a) FROM Alert a 
        WHERE a.resolved = false 
        GROUP BY a.severity
        """)
    List<Object[]> countActiveGroupedBySeverity();

    // Find alerts for auto-resolution check
    @Query("""
        SELECT a FROM Alert a 
        WHERE a.resolved = false 
        AND a.sensorId = :sensorId 
        AND a.alertCode = :alertCode
        """)
    List<Alert> findActiveAlertsBySensorAndCode(
            @Param("sensorId") UUID sensorId,
            @Param("alertCode") String alertCode);
}
