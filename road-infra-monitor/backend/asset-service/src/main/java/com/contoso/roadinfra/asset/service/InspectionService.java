package com.contoso.roadinfra.asset.service;

import com.contoso.roadinfra.asset.dto.InspectionCreateRequest;
import com.contoso.roadinfra.asset.dto.InspectionResponse;
import com.contoso.roadinfra.asset.entity.Asset;
import com.contoso.roadinfra.asset.entity.AssetInspection;
import com.contoso.roadinfra.asset.mapper.InspectionMapper;
import com.contoso.roadinfra.asset.repository.AssetRepository;
import com.contoso.roadinfra.asset.repository.InspectionRepository;
import com.contoso.roadinfra.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for managing asset inspections.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class InspectionService {

    private final InspectionRepository inspectionRepository;
    private final AssetRepository assetRepository;
    private final InspectionMapper inspectionMapper;
    private final AssetService assetService;

    /**
     * Record a new inspection for an asset.
     */
    @CacheEvict(value = {"inspections", "overdueInspections"}, allEntries = true)
    public InspectionResponse createInspection(UUID assetId, InspectionCreateRequest request, String username) {
        log.info("Creating inspection for asset {} by {}", assetId, username);

        Asset asset = assetRepository.findById(assetId)
                .orElseThrow(() -> new ResourceNotFoundException("Asset", assetId));

        AssetInspection inspection = inspectionMapper.toEntity(request);
        inspection.setAsset(asset);
        inspection.setCreatedBy(username);
        inspection.setUpdatedBy(username);

        AssetInspection saved = inspectionRepository.save(inspection);

        // Update asset's last inspection date and next scheduled date
        asset.setLastInspectionDate(saved.getInspectionDate());
        asset.setNextInspectionDate(calculateNextInspectionDate(saved.getInspectionDate(), asset));
        assetRepository.save(asset);

        // If inspection rating is low, update health status
        if (saved.getOverallConditionRating() <= 2) {
            log.warn("Low inspection rating ({}) for asset {}, consider updating health status",
                    saved.getOverallConditionRating(), assetId);
        }

        log.info("Inspection {} created for asset {}", saved.getId(), assetId);
        return inspectionMapper.toResponse(saved);
    }

    /**
     * Get inspections for an asset.
     */
    @Cacheable(value = "inspections", key = "#assetId + '-' + #pageable.pageNumber")
    @Transactional(readOnly = true)
    public Page<InspectionResponse> getInspectionsForAsset(UUID assetId, Pageable pageable) {
        log.debug("Fetching inspections for asset {}", assetId);

        // Verify asset exists
        if (!assetRepository.existsById(assetId)) {
            throw new ResourceNotFoundException("Asset", assetId);
        }

        return inspectionRepository.findByAssetIdOrderByInspectionDateDesc(assetId, pageable)
                .map(inspectionMapper::toResponse);
    }

    /**
     * Get a specific inspection by ID.
     */
    @Transactional(readOnly = true)
    public InspectionResponse getInspectionById(UUID inspectionId) {
        log.debug("Fetching inspection {}", inspectionId);
        AssetInspection inspection = inspectionRepository.findById(inspectionId)
                .orElseThrow(() -> new ResourceNotFoundException("Inspection", inspectionId));
        return inspectionMapper.toResponse(inspection);
    }

    /**
     * Get assets with overdue inspections.
     */
    @Cacheable(value = "overdueInspections")
    @Transactional(readOnly = true)
    public List<AssetInspectionOverdue> getOverdueInspections() {
        log.debug("Fetching assets with overdue inspections");

        LocalDate today = LocalDate.now();
        List<Asset> overdueAssets = assetRepository.findAssetsWithOverdueInspection(today);

        return overdueAssets.stream()
                .map(asset -> {
                    AssetInspection lastInspection = inspectionRepository
                            .findFirstByAssetIdOrderByInspectionDateDesc(asset.getId())
                            .orElse(null);

                    return AssetInspectionOverdue.builder()
                            .assetId(asset.getId())
                            .assetCode(asset.getAssetCode())
                            .assetName(asset.getName())
                            .assetType(asset.getAssetType())
                            .nextInspectionDate(asset.getNextInspectionDate())
                            .daysOverdue(calculateDaysOverdue(asset.getNextInspectionDate()))
                            .lastInspectionDate(lastInspection != null ?
                                    lastInspection.getInspectionDate() : null)
                            .lastInspectionRating(lastInspection != null ?
                                    lastInspection.getOverallConditionRating() : null)
                            .build();
                })
                .sorted((a, b) -> Integer.compare(b.getDaysOverdue(), a.getDaysOverdue()))
                .collect(Collectors.toList());
    }

    /**
     * Get inspection statistics for an asset.
     */
    @Transactional(readOnly = true)
    public InspectionStatistics getInspectionStatistics(UUID assetId) {
        log.debug("Calculating inspection statistics for asset {}", assetId);

        List<AssetInspection> inspections = inspectionRepository.findByAssetIdOrderByInspectionDateDesc(assetId);

        if (inspections.isEmpty()) {
            return InspectionStatistics.builder()
                    .totalInspections(0)
                    .averageRating(0.0)
                    .lastInspectionDate(null)
                    .trendDirection(TrendDirection.STABLE)
                    .build();
        }

        double avgRating = inspections.stream()
                .mapToInt(AssetInspection::getOverallConditionRating)
                .average()
                .orElse(0.0);

        TrendDirection trend = calculateTrend(inspections);

        return InspectionStatistics.builder()
                .totalInspections(inspections.size())
                .averageRating(avgRating)
                .lastInspectionDate(inspections.get(0).getInspectionDate())
                .latestRating(inspections.get(0).getOverallConditionRating())
                .trendDirection(trend)
                .build();
    }

    /**
     * Schedule follow-up inspections for assets with low ratings.
     */
    @Async
    public void scheduleFollowUpInspections() {
        log.info("Scheduling follow-up inspections for low-rated assets");

        LocalDate cutoff = LocalDate.now().minusDays(30);
        List<AssetInspection> recentLowRated = inspectionRepository
                .findByInspectionDateAfterAndOverallConditionRatingLessThanEqual(cutoff, 2);

        for (AssetInspection inspection : recentLowRated) {
            Asset asset = inspection.getAsset();
            LocalDate followUpDate = inspection.getInspectionDate().plusDays(14);

            if (asset.getNextInspectionDate() == null ||
                    asset.getNextInspectionDate().isAfter(followUpDate)) {
                asset.setNextInspectionDate(followUpDate);
                assetRepository.save(asset);
                log.info("Scheduled follow-up inspection for asset {} on {}",
                        asset.getAssetCode(), followUpDate);
            }
        }
    }

    // Helper methods

    private LocalDate calculateNextInspectionDate(LocalDate lastInspection, Asset asset) {
        // Default inspection interval based on asset type
        int intervalDays = switch (asset.getAssetType()) {
            case BRIDGE, TUNNEL -> 180; // 6 months
            case ROAD_SECTION, INTERCHANGE -> 365; // 1 year
            default -> 90; // 3 months for other types
        };

        return lastInspection.plusDays(intervalDays);
    }

    private int calculateDaysOverdue(LocalDate nextInspectionDate) {
        if (nextInspectionDate == null) {
            return 0;
        }
        return (int) java.time.temporal.ChronoUnit.DAYS.between(nextInspectionDate, LocalDate.now());
    }

    private TrendDirection calculateTrend(List<AssetInspection> inspections) {
        if (inspections.size() < 2) {
            return TrendDirection.STABLE;
        }

        // Compare last 3 inspections average vs previous 3
        int recentCount = Math.min(3, inspections.size() / 2);
        double recentAvg = inspections.subList(0, recentCount).stream()
                .mapToInt(AssetInspection::getOverallConditionRating)
                .average()
                .orElse(0.0);

        double previousAvg = inspections.subList(recentCount, Math.min(recentCount * 2, inspections.size())).stream()
                .mapToInt(AssetInspection::getOverallConditionRating)
                .average()
                .orElse(0.0);

        if (recentAvg > previousAvg + 0.5) {
            return TrendDirection.IMPROVING;
        } else if (recentAvg < previousAvg - 0.5) {
            return TrendDirection.DECLINING;
        }
        return TrendDirection.STABLE;
    }

    // Inner classes for response types

    @lombok.Builder
    @lombok.Data
    public static class AssetInspectionOverdue {
        private UUID assetId;
        private String assetCode;
        private String assetName;
        private com.contoso.roadinfra.common.constants.AssetType assetType;
        private LocalDate nextInspectionDate;
        private int daysOverdue;
        private LocalDate lastInspectionDate;
        private Integer lastInspectionRating;
    }

    @lombok.Builder
    @lombok.Data
    public static class InspectionStatistics {
        private int totalInspections;
        private double averageRating;
        private LocalDate lastInspectionDate;
        private Integer latestRating;
        private TrendDirection trendDirection;
    }

    public enum TrendDirection {
        IMPROVING, STABLE, DECLINING
    }
}
