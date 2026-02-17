package com.contoso.roadinfra.asset.config;

import com.contoso.roadinfra.asset.constants.ConstructionStatus;
import com.contoso.roadinfra.asset.constants.InspectionType;
import com.contoso.roadinfra.asset.constants.MilestoneStatus;
import com.contoso.roadinfra.asset.entity.Asset;
import com.contoso.roadinfra.asset.entity.AssetInspection;
import com.contoso.roadinfra.asset.entity.ConstructionMilestone;
import com.contoso.roadinfra.asset.repository.AssetRepository;
import com.contoso.roadinfra.asset.repository.InspectionRepository;
import com.contoso.roadinfra.asset.repository.MilestoneRepository;
import com.contoso.roadinfra.common.constants.AssetType;
import com.contoso.roadinfra.common.constants.HealthStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Data loader for seeding the database with sample assets for a 2km corridor.
 * Run only with 'dev', 'local', 'default', or 'docker' profile.
 */
@Component
@Profile({"dev", "local", "default", "docker"})
@RequiredArgsConstructor
@Slf4j
public class DataLoader implements CommandLineRunner {

    private final AssetRepository assetRepository;
    private final InspectionRepository inspectionRepository;
    private final MilestoneRepository milestoneRepository;

    // Corridor base coordinates (Seattle area)
    private static final double BASE_LAT = 47.6062;
    private static final double BASE_LON = -122.3321;
    private static final double KM_TO_LAT = 0.009; // ~1km in latitude degrees
    private static final double KM_TO_LON = 0.012; // ~1km in longitude degrees at this latitude

    @Override
    @Transactional
    public void run(String... args) {
        if (assetRepository.count() > 0) {
            log.info("Database already seeded, skipping data loading");
            return;
        }

        log.info("Starting to seed asset database with 2km corridor data...");

        List<Asset> allAssets = new ArrayList<>();

        // Create 3 road sections (~400m each, total ~1.2km of main road)
        Asset roadSection1 = createRoadSection("RS-001", "Highway 101 - Section A",
                0.0, 0.4, ConstructionStatus.COMPLETED, HealthStatus.HEALTHY, 100.0);
        Asset roadSection2 = createRoadSection("RS-002", "Highway 101 - Section B",
                0.4, 0.8, ConstructionStatus.COMPLETED, HealthStatus.WARNING, 100.0);
        Asset roadSection3 = createRoadSection("RS-003", "Highway 101 - Section C",
                0.8, 1.2, ConstructionStatus.IN_PROGRESS, HealthStatus.UNKNOWN, 65.0);

        allAssets.add(roadSection1);
        allAssets.add(roadSection2);
        allAssets.add(roadSection3);

        // Create 1 bridge with 4 children (starts at km 1.2, ends at km 1.5)
        Asset bridge = createBridge("BR-001", "Contoso River Bridge",
                1.2, 1.5, ConstructionStatus.COMPLETED, HealthStatus.HEALTHY, 100.0);
        allAssets.add(bridge);

        // Save bridge first to get ID for children
        Asset savedBridge = assetRepository.save(bridge);

        // Bridge children
        Asset pier1 = createBridgeChild("BR-001-P1", "Bridge Pier North", savedBridge.getId(),
                1.25, AssetType.BRIDGE, ConstructionStatus.COMPLETED, HealthStatus.HEALTHY, 100.0);
        Asset pier2 = createBridgeChild("BR-001-P2", "Bridge Pier South", savedBridge.getId(),
                1.45, AssetType.BRIDGE, ConstructionStatus.COMPLETED, HealthStatus.HEALTHY, 100.0);
        Asset deck = createBridgeChild("BR-001-DK", "Bridge Deck", savedBridge.getId(),
                1.35, AssetType.BRIDGE, ConstructionStatus.COMPLETED, HealthStatus.WARNING, 100.0);
        Asset abutment = createBridgeChild("BR-001-AB", "Bridge Abutment", savedBridge.getId(),
                1.2, AssetType.BRIDGE, ConstructionStatus.COMPLETED, HealthStatus.HEALTHY, 100.0);

        allAssets.add(pier1);
        allAssets.add(pier2);
        allAssets.add(deck);
        allAssets.add(abutment);

        // Create 1 tunnel with 3 child sections (km 1.5 to km 1.8)
        Asset tunnel = createTunnel("TN-001", "Mountain Pass Tunnel",
                1.5, 1.8, ConstructionStatus.IN_PROGRESS, HealthStatus.UNKNOWN, 40.0);
        allAssets.add(tunnel);

        // Save tunnel first to get ID for children
        Asset savedTunnel = assetRepository.save(tunnel);

        // Tunnel children
        Asset tunnelSection1 = createTunnelChild("TN-001-S1", "Tunnel Section Entry", savedTunnel.getId(),
                1.5, 1.6, ConstructionStatus.IN_PROGRESS, HealthStatus.UNKNOWN, 60.0);
        Asset tunnelSection2 = createTunnelChild("TN-001-S2", "Tunnel Section Middle", savedTunnel.getId(),
                1.6, 1.7, ConstructionStatus.IN_PROGRESS, HealthStatus.UNKNOWN, 30.0);
        Asset tunnelSection3 = createTunnelChild("TN-001-S3", "Tunnel Section Exit", savedTunnel.getId(),
                1.7, 1.8, ConstructionStatus.PLANNED, HealthStatus.UNKNOWN, 0.0);

        allAssets.add(tunnelSection1);
        allAssets.add(tunnelSection2);
        allAssets.add(tunnelSection3);

        // Create 2 drainage systems (at km 0.5 and km 1.0)
        Asset drainage1 = createDrainage("DR-001", "Drainage System A",
                0.5, ConstructionStatus.COMPLETED, HealthStatus.HEALTHY, 100.0);
        Asset drainage2 = createDrainage("DR-002", "Drainage System B",
                1.0, ConstructionStatus.COMPLETED, HealthStatus.CRITICAL, 100.0);

        allAssets.add(drainage1);
        allAssets.add(drainage2);

        // Create 2 guardrail segments (km 0.0-0.4 and km 1.8-2.0)
        Asset guardrail1 = createGuardrail("GR-001", "Guardrail Section West",
                0.0, 0.4, ConstructionStatus.COMPLETED, HealthStatus.HEALTHY, 100.0);
        Asset guardrail2 = createGuardrail("GR-002", "Guardrail Section East",
                1.8, 2.0, ConstructionStatus.PLANNED, HealthStatus.UNKNOWN, 0.0);

        allAssets.add(guardrail1);
        allAssets.add(guardrail2);

        // Save all non-parent assets
        List<Asset> savedAssets = assetRepository.saveAll(allAssets);
        log.info("Saved {} assets", savedAssets.size() + 2); // +2 for bridge and tunnel saved earlier

        // Create milestones for assets under construction
        createMilestonesForAsset(roadSection3, "Road Section C");
        createMilestonesForAsset(savedTunnel, "Mountain Tunnel");
        createMilestonesForAsset(tunnelSection1, "Tunnel Entry");
        createMilestonesForAsset(tunnelSection2, "Tunnel Middle");
        createMilestonesForAsset(tunnelSection3, "Tunnel Exit");
        createMilestonesForAsset(guardrail2, "Guardrail East");

        // Create sample inspections
        createSampleInspections(roadSection1);
        createSampleInspections(roadSection2);
        createSampleInspections(savedBridge);
        createSampleInspections(deck);
        createSampleInspections(drainage2);

        log.info("Asset database seeding completed successfully!");
    }

    private Asset createRoadSection(String code, String name, double startKm, double endKm,
                                    ConstructionStatus status, HealthStatus health, double completion) {
        return Asset.builder()
                .assetCode(code)
                .name(name)
                .description("Road section from km " + startKm + " to km " + endKm)
                .assetType(AssetType.ROAD_SECTION)
                .status(status)
                .healthStatus(health)
                .completionPercentage(completion)
                .startChainage(startKm)
                .endChainage(endKm)
                .startLatitude(BASE_LAT + (startKm * KM_TO_LAT))
                .startLongitude(BASE_LON + (startKm * KM_TO_LON))
                .endLatitude(BASE_LAT + (endKm * KM_TO_LAT))
                .endLongitude(BASE_LON + (endKm * KM_TO_LON))
                .constructionStartDate(LocalDate.now().minusMonths(6))
                .expectedCompletionDate(status == ConstructionStatus.COMPLETED ?
                        LocalDate.now().minusMonths(1) : LocalDate.now().plusMonths(3))
                .constructionEndDate(status == ConstructionStatus.COMPLETED ?
                        LocalDate.now().minusMonths(1) : null)
                .lastInspectionDate(status == ConstructionStatus.COMPLETED ?
                        LocalDate.now().minusDays(30) : null)
                .nextInspectionDate(status == ConstructionStatus.COMPLETED ?
                        LocalDate.now().plusMonths(6) : null)
                .createdBy("system")
                .updatedBy("system")
                .build();
    }

    private Asset createBridge(String code, String name, double startKm, double endKm,
                               ConstructionStatus status, HealthStatus health, double completion) {
        return Asset.builder()
                .assetCode(code)
                .name(name)
                .description("Bridge spanning from km " + startKm + " to km " + endKm)
                .assetType(AssetType.BRIDGE)
                .status(status)
                .healthStatus(health)
                .completionPercentage(completion)
                .startChainage(startKm)
                .endChainage(endKm)
                .startLatitude(BASE_LAT + (startKm * KM_TO_LAT))
                .startLongitude(BASE_LON + (startKm * KM_TO_LON))
                .endLatitude(BASE_LAT + (endKm * KM_TO_LAT))
                .endLongitude(BASE_LON + (endKm * KM_TO_LON))
                .constructionStartDate(LocalDate.now().minusYears(2))
                .expectedCompletionDate(LocalDate.now().minusYears(1))
                .constructionEndDate(LocalDate.now().minusYears(1))
                .lastInspectionDate(LocalDate.now().minusDays(60))
                .nextInspectionDate(LocalDate.now().plusMonths(4))
                .createdBy("system")
                .updatedBy("system")
                .build();
    }

    private Asset createBridgeChild(String code, String name, UUID parentId, double chainage,
                                    AssetType type, ConstructionStatus status, HealthStatus health, double completion) {
        return Asset.builder()
                .assetCode(code)
                .name(name)
                .description("Bridge component: " + name)
                .assetType(type)
                .parentAssetId(parentId)
                .status(status)
                .healthStatus(health)
                .completionPercentage(completion)
                .startChainage(chainage)
                .endChainage(chainage)
                .startLatitude(BASE_LAT + (chainage * KM_TO_LAT))
                .startLongitude(BASE_LON + (chainage * KM_TO_LON))
                .constructionStartDate(LocalDate.now().minusYears(2))
                .constructionEndDate(LocalDate.now().minusYears(1))
                .createdBy("system")
                .updatedBy("system")
                .build();
    }

    private Asset createTunnel(String code, String name, double startKm, double endKm,
                               ConstructionStatus status, HealthStatus health, double completion) {
        return Asset.builder()
                .assetCode(code)
                .name(name)
                .description("Tunnel from km " + startKm + " to km " + endKm)
                .assetType(AssetType.TUNNEL)
                .status(status)
                .healthStatus(health)
                .completionPercentage(completion)
                .startChainage(startKm)
                .endChainage(endKm)
                .startLatitude(BASE_LAT + (startKm * KM_TO_LAT))
                .startLongitude(BASE_LON + (startKm * KM_TO_LON))
                .endLatitude(BASE_LAT + (endKm * KM_TO_LAT))
                .endLongitude(BASE_LON + (endKm * KM_TO_LON))
                .constructionStartDate(LocalDate.now().minusMonths(4))
                .expectedCompletionDate(LocalDate.now().plusMonths(8))
                .createdBy("system")
                .updatedBy("system")
                .build();
    }

    private Asset createTunnelChild(String code, String name, UUID parentId, double startKm, double endKm,
                                    ConstructionStatus status, HealthStatus health, double completion) {
        return Asset.builder()
                .assetCode(code)
                .name(name)
                .description("Tunnel section: " + name)
                .assetType(AssetType.TUNNEL)
                .parentAssetId(parentId)
                .status(status)
                .healthStatus(health)
                .completionPercentage(completion)
                .startChainage(startKm)
                .endChainage(endKm)
                .startLatitude(BASE_LAT + (startKm * KM_TO_LAT))
                .startLongitude(BASE_LON + (startKm * KM_TO_LON))
                .endLatitude(BASE_LAT + (endKm * KM_TO_LAT))
                .endLongitude(BASE_LON + (endKm * KM_TO_LON))
                .constructionStartDate(LocalDate.now().minusMonths(3))
                .expectedCompletionDate(LocalDate.now().plusMonths(6))
                .createdBy("system")
                .updatedBy("system")
                .build();
    }

    private Asset createDrainage(String code, String name, double chainage,
                                 ConstructionStatus status, HealthStatus health, double completion) {
        return Asset.builder()
                .assetCode(code)
                .name(name)
                .description("Drainage system at km " + chainage)
                .assetType(AssetType.DRAINAGE)
                .status(status)
                .healthStatus(health)
                .completionPercentage(completion)
                .startChainage(chainage)
                .endChainage(chainage)
                .startLatitude(BASE_LAT + (chainage * KM_TO_LAT))
                .startLongitude(BASE_LON + (chainage * KM_TO_LON))
                .constructionStartDate(LocalDate.now().minusMonths(8))
                .constructionEndDate(LocalDate.now().minusMonths(5))
                .lastInspectionDate(health == HealthStatus.CRITICAL ?
                        LocalDate.now().minusDays(7) : LocalDate.now().minusDays(45))
                .nextInspectionDate(health == HealthStatus.CRITICAL ?
                        LocalDate.now().minusDays(23) : LocalDate.now().plusMonths(2)) // Overdue if critical
                .createdBy("system")
                .updatedBy("system")
                .build();
    }

    private Asset createGuardrail(String code, String name, double startKm, double endKm,
                                  ConstructionStatus status, HealthStatus health, double completion) {
        return Asset.builder()
                .assetCode(code)
                .name(name)
                .description("Guardrail from km " + startKm + " to km " + endKm)
                .assetType(AssetType.GUARDRAIL)
                .status(status)
                .healthStatus(health)
                .completionPercentage(completion)
                .startChainage(startKm)
                .endChainage(endKm)
                .startLatitude(BASE_LAT + (startKm * KM_TO_LAT))
                .startLongitude(BASE_LON + (startKm * KM_TO_LON))
                .endLatitude(BASE_LAT + (endKm * KM_TO_LAT))
                .endLongitude(BASE_LON + (endKm * KM_TO_LON))
                .constructionStartDate(status == ConstructionStatus.COMPLETED ?
                        LocalDate.now().minusMonths(10) : LocalDate.now().plusWeeks(2))
                .expectedCompletionDate(status == ConstructionStatus.COMPLETED ?
                        LocalDate.now().minusMonths(8) : LocalDate.now().plusMonths(4))
                .constructionEndDate(status == ConstructionStatus.COMPLETED ?
                        LocalDate.now().minusMonths(8) : null)
                .createdBy("system")
                .updatedBy("system")
                .build();
    }

    private void createMilestonesForAsset(Asset asset, String prefix) {
        if (asset.getStatus() == ConstructionStatus.COMPLETED) {
            return; // No milestones needed for completed assets
        }

        List<ConstructionMilestone> milestones = new ArrayList<>();

        // Planning phase
        ConstructionMilestone planning = ConstructionMilestone.builder()
                .asset(asset)
                .name(prefix + " - Planning Complete")
                .description("Complete planning and design phase")
                .plannedDate(LocalDate.now().minusMonths(3))
                .actualCompletionDate(LocalDate.now().minusMonths(3))
                .status(MilestoneStatus.COMPLETED)
                .weight(0.1)
                .createdBy("system")
                .updatedBy("system")
                .build();
        milestones.add(planning);

        // Site preparation
        ConstructionMilestone sitePrepMilestone = ConstructionMilestone.builder()
                .asset(asset)
                .name(prefix + " - Site Preparation")
                .description("Complete site preparation work")
                .plannedDate(LocalDate.now().minusMonths(2))
                .actualCompletionDate(LocalDate.now().minusMonths(2).plusDays(5))
                .status(MilestoneStatus.COMPLETED)
                .weight(0.15)
                .createdBy("system")
                .updatedBy("system")
                .build();
        milestones.add(sitePrepMilestone);

        // Foundation work
        ConstructionMilestone foundation = ConstructionMilestone.builder()
                .asset(asset)
                .name(prefix + " - Foundation Work")
                .description("Complete foundation construction")
                .plannedDate(LocalDate.now().minusWeeks(4))
                .status(asset.getCompletionPercentage() >= 30 ?
                        MilestoneStatus.COMPLETED : MilestoneStatus.IN_PROGRESS)
                .actualCompletionDate(asset.getCompletionPercentage() >= 30 ?
                        LocalDate.now().minusWeeks(3) : null)
                .weight(0.25)
                .createdBy("system")
                .updatedBy("system")
                .build();
        milestones.add(foundation);

        // Main structure
        ConstructionMilestone mainStructure = ConstructionMilestone.builder()
                .asset(asset)
                .name(prefix + " - Main Structure")
                .description("Complete main structural work")
                .plannedDate(LocalDate.now().plusWeeks(2))
                .status(asset.getCompletionPercentage() >= 60 ?
                        MilestoneStatus.IN_PROGRESS : MilestoneStatus.PENDING)
                .weight(0.30)
                .createdBy("system")
                .updatedBy("system")
                .build();
        milestones.add(mainStructure);

        // Finishing work
        ConstructionMilestone finishing = ConstructionMilestone.builder()
                .asset(asset)
                .name(prefix + " - Finishing Work")
                .description("Complete finishing and surface work")
                .plannedDate(LocalDate.now().plusMonths(2))
                .status(MilestoneStatus.PENDING)
                .weight(0.15)
                .createdBy("system")
                .updatedBy("system")
                .build();
        milestones.add(finishing);

        // Final inspection
        ConstructionMilestone finalInspection = ConstructionMilestone.builder()
                .asset(asset)
                .name(prefix + " - Final Inspection")
                .description("Pass final safety inspection")
                .plannedDate(LocalDate.now().plusMonths(3))
                .status(MilestoneStatus.PENDING)
                .weight(0.05)
                .createdBy("system")
                .updatedBy("system")
                .build();
        milestones.add(finalInspection);

        milestoneRepository.saveAll(milestones);
        log.info("Created {} milestones for asset {}", milestones.size(), asset.getAssetCode());
    }

    private void createSampleInspections(Asset asset) {
        List<AssetInspection> inspections = new ArrayList<>();

        // Recent inspection
        AssetInspection recentInspection = AssetInspection.builder()
                .asset(asset)
                .inspectorName("John Smith")
                .inspectionType(InspectionType.ROUTINE)
                .inspectionDate(LocalDate.now().minusDays(30))
                .overallConditionRating(asset.getHealthStatus() == HealthStatus.CRITICAL ? 2 :
                        asset.getHealthStatus() == HealthStatus.WARNING ? 3 : 4)
                .findings(asset.getHealthStatus() == HealthStatus.CRITICAL ?
                        "Significant structural concerns identified. Immediate attention required." :
                        asset.getHealthStatus() == HealthStatus.WARNING ?
                                "Minor surface wear detected. Schedule maintenance within 3 months." :
                                "Asset in good condition. No immediate action required.")
                .recommendations(asset.getHealthStatus() == HealthStatus.CRITICAL ?
                        "1. Conduct detailed structural assessment\n2. Implement temporary safety measures\n3. Plan for repairs" :
                        "Continue regular monitoring and maintenance schedule.")
                .nextInspectionRecommendedDate(LocalDate.now().plusMonths(
                        asset.getHealthStatus() == HealthStatus.CRITICAL ? 1 :
                                asset.getHealthStatus() == HealthStatus.WARNING ? 3 : 6))
                .createdBy("system")
                .updatedBy("system")
                .build();
        inspections.add(recentInspection);

        // Older inspection (6 months ago)
        AssetInspection olderInspection = AssetInspection.builder()
                .asset(asset)
                .inspectorName("Jane Doe")
                .inspectionType(InspectionType.ROUTINE)
                .inspectionDate(LocalDate.now().minusMonths(6))
                .overallConditionRating(4)
                .findings("Asset in good condition at time of inspection.")
                .recommendations("Continue regular maintenance schedule.")
                .nextInspectionRecommendedDate(LocalDate.now())
                .createdBy("system")
                .updatedBy("system")
                .build();
        inspections.add(olderInspection);

        inspectionRepository.saveAll(inspections);
        log.info("Created {} inspections for asset {}", inspections.size(), asset.getAssetCode());
    }
}
