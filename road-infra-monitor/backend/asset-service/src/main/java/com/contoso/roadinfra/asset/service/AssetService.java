package com.contoso.roadinfra.asset.service;

import com.contoso.roadinfra.asset.client.SensorServiceClient;
import com.contoso.roadinfra.asset.constants.ConstructionStatus;
import com.contoso.roadinfra.asset.dto.*;
import com.contoso.roadinfra.asset.entity.Asset;
import com.contoso.roadinfra.asset.event.AssetEventPublisher;
import com.contoso.roadinfra.asset.mapper.AssetMapper;
import com.contoso.roadinfra.asset.repository.AssetRepository;
import com.contoso.roadinfra.asset.repository.MilestoneRepository;
import com.contoso.roadinfra.common.constants.AssetType;
import com.contoso.roadinfra.common.constants.HealthStatus;
import com.contoso.roadinfra.common.dto.ApiResponse;
import com.contoso.roadinfra.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for managing infrastructure assets.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AssetService {

    private final AssetRepository assetRepository;
    private final MilestoneRepository milestoneRepository;
    private final AssetMapper assetMapper;
    private final AssetEventPublisher eventPublisher;
    private final SensorServiceClient sensorServiceClient;

    /**
     * Get all assets with pagination.
     */
    @Transactional(readOnly = true)
    public Page<AssetResponse> getAllAssets(Pageable pageable) {
        log.debug("Fetching all assets with pagination");
        return assetRepository.findAll(pageable)
                .map(this::enrichAssetResponse);
    }

    /**
     * Get asset by ID with children and sensor count.
     */
    @Cacheable(value = "assets", key = "#id")
    @Transactional(readOnly = true)
    public AssetResponse getAssetById(UUID id) {
        log.debug("Fetching asset with id: {}", id);
        Asset asset = assetRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Asset", id));
        return enrichAssetResponseWithDetails(asset);
    }

    /**
     * Get asset by code.
     */
    @Cacheable(value = "assetsByCode", key = "#assetCode")
    @Transactional(readOnly = true)
    public AssetResponse getAssetByCode(String assetCode) {
        log.debug("Fetching asset with code: {}", assetCode);
        Asset asset = assetRepository.findByAssetCode(assetCode)
                .orElseThrow(() -> new ResourceNotFoundException("Asset", assetCode));
        return enrichAssetResponseWithDetails(asset);
    }

    /**
     * Create a new asset.
     */
    @CacheEvict(value = {"assets", "assetsByCode", "corridorSummary", "geoJson"}, allEntries = true)
    public AssetResponse createAsset(AssetCreateRequest request, String username) {
        log.info("Creating new asset: {} by user {}", request.getAssetCode(), username);

        // Check for duplicate asset code
        if (assetRepository.existsByAssetCode(request.getAssetCode())) {
            throw new IllegalArgumentException("Asset with code " + request.getAssetCode() + " already exists");
        }

        Asset asset = assetMapper.toEntity(request);
        asset.setCompletionPercentage(0.0);
        if (asset.getStatus() == null) {
            asset.setStatus(ConstructionStatus.PLANNED);
        }
        if (asset.getHealthStatus() == null) {
            asset.setHealthStatus(HealthStatus.UNKNOWN);
        }
        asset.setCreatedBy(username);
        asset.setUpdatedBy(username);

        Asset saved = assetRepository.save(asset);
        log.info("Asset created with ID: {}", saved.getId());

        // Publish event
        eventPublisher.publishAssetCreated(saved);

        return enrichAssetResponse(saved);
    }

    /**
     * Update an existing asset.
     */
    @CacheEvict(value = {"assets", "assetsByCode", "corridorSummary", "geoJson"}, allEntries = true)
    public AssetResponse updateAsset(UUID id, AssetUpdateRequest request, String username) {
        log.info("Updating asset with id: {} by user {}", id, username);
        Asset asset = assetRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Asset", id));

        assetMapper.updateEntity(request, asset);
        asset.setUpdatedBy(username);

        Asset updated = assetRepository.save(asset);
        log.info("Asset {} updated successfully", id);

        // Publish event
        eventPublisher.publishAssetUpdated(updated);

        return enrichAssetResponse(updated);
    }

    /**
     * Update asset completion percentage.
     */
    @CacheEvict(value = {"assets", "assetsByCode", "corridorSummary"}, allEntries = true)
    public AssetResponse updateProgress(UUID id, AssetProgressUpdateRequest request, String username) {
        log.info("Updating progress of asset {} to {}% by user {}", id, request.getCompletionPercentage(), username);
        Asset asset = assetRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Asset", id));

        if (asset.getStatus() != ConstructionStatus.IN_PROGRESS) {
            throw new IllegalStateException("Progress can only be updated for assets with IN_PROGRESS status");
        }

        Double oldProgress = asset.getCompletionPercentage();
        asset.setCompletionPercentage(request.getCompletionPercentage());
        asset.setUpdatedBy(username);

        // Auto-complete if 100%
        if (request.getCompletionPercentage() >= 100.0) {
            asset.setCompletionPercentage(100.0);
            asset.setStatus(ConstructionStatus.COMPLETED);
            asset.setConstructionEndDate(LocalDate.now());
        }

        Asset updated = assetRepository.save(asset);
        log.info("Asset {} progress updated from {}% to {}%", id, oldProgress, request.getCompletionPercentage());

        // Publish event
        eventPublisher.publishProgressUpdate(updated, oldProgress, updated.getCompletionPercentage(), request.getNotes());

        return enrichAssetResponse(updated);
    }

    /**
     * Update asset health status.
     */
    @CacheEvict(value = {"assets", "assetsByCode", "corridorSummary"}, allEntries = true)
    public AssetResponse updateHealthStatus(UUID id, AssetHealthUpdateRequest request, String username) {
        log.info("Updating health status of asset {} to {} by user {}", id, request.getHealthStatus(), username);
        Asset asset = assetRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Asset", id));

        HealthStatus oldStatus = asset.getHealthStatus();
        asset.setHealthStatus(request.getHealthStatus());
        asset.setUpdatedBy(username);

        Asset updated = assetRepository.save(asset);
        log.info("Asset {} health status changed from {} to {}", id, oldStatus, request.getHealthStatus());

        // Publish event to Kafka
        eventPublisher.publishHealthStatusChange(updated, oldStatus, request.getReason());

        return enrichAssetResponse(updated);
    }

    /**
     * Delete an asset.
     */
    @CacheEvict(value = {"assets", "assetsByCode", "assetChildren", "corridorSummary", "geoJson"}, allEntries = true)
    public void deleteAsset(UUID id, String username) {
        log.info("Deleting asset with id: {} by user {}", id, username);
        Asset asset = assetRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Asset", id));

        // Check for child assets
        long childCount = assetRepository.countByParentAssetId(id);
        if (childCount > 0) {
            throw new IllegalStateException("Cannot delete asset with " + childCount + " child assets. Delete children first.");
        }

        assetRepository.delete(asset);
        log.info("Asset {} deleted by {}", id, username);

        // Publish event
        eventPublisher.publishAssetDeleted(asset);
    }

    /**
     * Get child assets.
     */
    @Cacheable(value = "assetChildren", key = "#parentId")
    @Transactional(readOnly = true)
    public List<AssetResponse> getChildAssets(UUID parentId) {
        log.debug("Fetching child assets for parent: {}", parentId);
        // Verify parent exists
        if (!assetRepository.existsById(parentId)) {
            throw new ResourceNotFoundException("Asset", parentId);
        }

        List<Asset> children = assetRepository.findByParentAssetId(parentId);
        return children.stream()
                .map(this::enrichAssetResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get corridor summary.
     */
    @Cacheable(value = "corridorSummary")
    @Transactional(readOnly = true)
    public CorridorSummaryResponse getCorridorSummary() {
        log.debug("Generating corridor summary");

        Double totalLength = assetRepository.getTotalCorridorLength();
        long totalAssets = assetRepository.count();
        Double avgCompletion = assetRepository.getAverageCompletionPercentage();

        // Count by type
        Map<AssetType, Long> byType = assetRepository.countGroupedByType().stream()
                .collect(Collectors.toMap(
                        row -> (AssetType) row[0],
                        row -> (Long) row[1]
                ));

        // Count by health status
        Map<HealthStatus, Long> byHealth = assetRepository.countGroupedByHealthStatus().stream()
                .collect(Collectors.toMap(
                        row -> (HealthStatus) row[0],
                        row -> (Long) row[1]
                ));

        // Count overdue inspections
        int overdueInspections = assetRepository.findAssetsWithOverdueInspection(LocalDate.now()).size();

        // Count delayed milestones
        int delayedMilestones = milestoneRepository.findDelayedMilestones(LocalDate.now()).size();

        // Count critical assets
        long criticalAssets = byHealth.getOrDefault(HealthStatus.CRITICAL, 0L);

        // Determine overall health
        HealthStatus overallHealth = determineOverallHealth(byHealth);

        // Get total sensor count (sum from all assets)
        int totalSensors = getTotalSensorCount();

        return CorridorSummaryResponse.builder()
                .totalLength(totalLength != null ? totalLength : 0.0)
                .totalAssets((int) totalAssets)
                .averageCompletionPercentage(avgCompletion != null ? avgCompletion : 0.0)
                .assetCountByType(byType)
                .assetCountByHealthStatus(byHealth)
                .totalSensors(totalSensors)
                .overdueInspections(overdueInspections)
                .delayedMilestones(delayedMilestones)
                .criticalAssets((int) criticalAssets)
                .overallHealth(overallHealth)
                .build();
    }

    /**
     * Get GeoJSON feature collection for all assets.
     */
    @Cacheable(value = "geoJson")
    @Transactional(readOnly = true)
    public GeoJsonFeatureCollection getGeoJsonFeatures() {
        log.debug("Generating GeoJSON features for all assets");

        List<Asset> assets = assetRepository.findAll();
        List<GeoJsonFeatureCollection.Feature> features = assets.stream()
                .map(this::assetToGeoJsonFeature)
                .collect(Collectors.toList());

        return GeoJsonFeatureCollection.builder()
                .type("FeatureCollection")
                .features(features)
                .build();
    }

    /**
     * Recalculate completion percentage based on milestones.
     */
    @CacheEvict(value = {"assets", "assetsByCode", "corridorSummary"}, allEntries = true)
    public Double recalculateCompletionFromMilestones(UUID assetId) {
        long total = milestoneRepository.countByAssetId(assetId);
        if (total == 0) {
            return 0.0;
        }

        long completed = milestoneRepository.countCompletedByAssetId(assetId);
        double percentage = (completed * 100.0) / total;

        Asset asset = assetRepository.findById(assetId)
                .orElseThrow(() -> new ResourceNotFoundException("Asset", assetId));

        Double oldPercentage = asset.getCompletionPercentage();
        asset.setCompletionPercentage(percentage);

        if (percentage >= 100.0) {
            asset.setStatus(ConstructionStatus.COMPLETED);
            asset.setConstructionEndDate(LocalDate.now());
        }

        assetRepository.save(asset);

        if (!Objects.equals(oldPercentage, percentage)) {
            eventPublisher.publishProgressUpdate(asset, oldPercentage, percentage, "Auto-calculated from milestones");
        }

        return percentage;
    }

    // Helper methods

    private AssetResponse enrichAssetResponse(Asset asset) {
        AssetResponse response = assetMapper.toResponse(asset);
        response.setChildAssetCount((int) assetRepository.countByParentAssetId(asset.getId()));
        return response;
    }

    private AssetResponse enrichAssetResponseWithDetails(Asset asset) {
        AssetResponse response = assetMapper.toResponse(asset);

        // Get child assets
        List<Asset> children = assetRepository.findByParentAssetId(asset.getId());
        response.setChildAssetCount(children.size());
        response.setChildren(assetMapper.toResponseList(children));

        // Get sensor count from sensor-service
        try {
            ApiResponse<Long> sensorResponse = sensorServiceClient.getSensorCountByAssetId(asset.getId());
            if (sensorResponse != null && sensorResponse.getData() != null) {
                response.setSensorCount(sensorResponse.getData().intValue());
            } else {
                response.setSensorCount(0);
            }
        } catch (Exception e) {
            log.warn("Failed to get sensor count for asset {}: {}", asset.getId(), e.getMessage());
            response.setSensorCount(0);
        }

        return response;
    }

    private GeoJsonFeatureCollection.Feature assetToGeoJsonFeature(Asset asset) {
        Map<String, Object> properties = new HashMap<>();
        properties.put("id", asset.getId().toString());
        properties.put("assetCode", asset.getAssetCode());
        properties.put("name", asset.getName());
        properties.put("assetType", asset.getAssetType().getDisplayName());
        properties.put("status", asset.getStatus().getDisplayName());
        properties.put("healthStatus", asset.getHealthStatus().getDisplayName());
        properties.put("completionPercentage", asset.getCompletionPercentage());
        properties.put("startChainage", asset.getStartChainage());
        properties.put("endChainage", asset.getEndChainage());

        GeoJsonFeatureCollection.Geometry geometry;
        if (asset.getEndLatitude() != null && asset.getEndLongitude() != null) {
            // LineString for linear assets
            double[][] coordinates = {
                    {asset.getStartLongitude(), asset.getStartLatitude()},
                    {asset.getEndLongitude(), asset.getEndLatitude()}
            };
            geometry = GeoJsonFeatureCollection.Geometry.builder()
                    .type("LineString")
                    .coordinates(coordinates)
                    .build();
        } else {
            // Point for single-location assets
            double[] coordinates = {asset.getStartLongitude(), asset.getStartLatitude()};
            geometry = GeoJsonFeatureCollection.Geometry.builder()
                    .type("Point")
                    .coordinates(coordinates)
                    .build();
        }

        return GeoJsonFeatureCollection.Feature.builder()
                .type("Feature")
                .id(asset.getId().toString())
                .geometry(geometry)
                .properties(properties)
                .build();
    }

    private HealthStatus determineOverallHealth(Map<HealthStatus, Long> healthCounts) {
        if (healthCounts.getOrDefault(HealthStatus.CRITICAL, 0L) > 0) {
            return HealthStatus.CRITICAL;
        }
        if (healthCounts.getOrDefault(HealthStatus.WARNING, 0L) > 0) {
            return HealthStatus.WARNING;
        }
        if (healthCounts.getOrDefault(HealthStatus.OFFLINE, 0L) > 0) {
            return HealthStatus.WARNING;
        }
        if (healthCounts.getOrDefault(HealthStatus.HEALTHY, 0L) > 0) {
            return HealthStatus.HEALTHY;
        }
        return HealthStatus.UNKNOWN;
    }

    private int getTotalSensorCount() {
        // This would ideally be a single call to sensor-service
        // For now, return 0 and let individual asset views fetch their own counts
        return 0;
    }
}
