package com.contoso.roadinfra.alert.repository;

import com.contoso.roadinfra.alert.entity.AlertRule;
import com.contoso.roadinfra.common.constants.AssetType;
import com.contoso.roadinfra.common.constants.SensorType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for AlertRule entities.
 */
@Repository
public interface AlertRuleRepository extends JpaRepository<AlertRule, UUID> {

    Optional<AlertRule> findByCode(String code);

    List<AlertRule> findByEnabledTrue();

    List<AlertRule> findByAssetTypeAndEnabledTrue(AssetType assetType);

    List<AlertRule> findBySensorTypeAndEnabledTrue(SensorType sensorType);

    List<AlertRule> findByAssetTypeAndSensorTypeAndEnabledTrue(AssetType assetType, SensorType sensorType);

    @Query("""
        SELECT r FROM AlertRule r 
        WHERE r.enabled = true 
        AND (r.assetType IS NULL OR r.assetType = :assetType)
        AND (r.sensorType IS NULL OR r.sensorType = :sensorType)
        AND (r.metricName IS NULL OR r.metricName = :metricName)
        ORDER BY r.priority ASC
        """)
    List<AlertRule> findMatchingRules(
            @Param("assetType") AssetType assetType,
            @Param("sensorType") SensorType sensorType,
            @Param("metricName") String metricName);

    boolean existsByCode(String code);

    @Query("SELECT r FROM AlertRule r WHERE r.enabled = true ORDER BY r.priority ASC")
    List<AlertRule> findAllEnabledOrderByPriority();
}
