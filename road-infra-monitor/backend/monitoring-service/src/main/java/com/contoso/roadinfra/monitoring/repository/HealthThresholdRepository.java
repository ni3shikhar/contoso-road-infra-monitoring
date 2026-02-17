package com.contoso.roadinfra.monitoring.repository;

import com.contoso.roadinfra.common.constants.AssetType;
import com.contoso.roadinfra.common.constants.SensorType;
import com.contoso.roadinfra.monitoring.entity.HealthThreshold;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface HealthThresholdRepository extends JpaRepository<HealthThreshold, UUID> {

    // Find threshold by asset type, sensor type, and metric
    Optional<HealthThreshold> findByAssetTypeAndSensorTypeAndMetricName(
            AssetType assetType, SensorType sensorType, String metricName);

    // Find all thresholds for an asset type
    List<HealthThreshold> findByAssetType(AssetType assetType);

    // Find all thresholds for a sensor type
    List<HealthThreshold> findBySensorType(SensorType sensorType);

    // Find all thresholds for an asset type and sensor type
    List<HealthThreshold> findByAssetTypeAndSensorType(AssetType assetType, SensorType sensorType);

    // Find all enabled thresholds
    List<HealthThreshold> findByEnabledTrue();

    // Find enabled thresholds for an asset type
    List<HealthThreshold> findByAssetTypeAndEnabledTrue(AssetType assetType);

    // Find enabled thresholds for asset type and sensor type
    List<HealthThreshold> findByAssetTypeAndSensorTypeAndEnabledTrue(AssetType assetType, SensorType sensorType);

    // Check if threshold exists
    boolean existsByAssetTypeAndSensorTypeAndMetricName(AssetType assetType, SensorType sensorType, String metricName);
}
